alter table public.projects
  add column website_url text,
  add column repository_url text,
  add column status_as_of date;

alter table public.projects drop constraint projects_stage_check;
alter table public.projects add constraint projects_stage_check
  check (stage in ('idea', 'prototype', 'alpha', 'beta', 'released', 'in_development', 'paused', 'archived'));
alter table public.projects add constraint projects_website_url_https
  check (website_url is null or (website_url ~ '^https://[^/[:space:]]+(/.*)?$' and length(website_url) <= 2048));
alter table public.projects add constraint projects_repository_url_https
  check (repository_url is null or (repository_url ~ '^https://[^/[:space:]]+(/.*)?$' and length(repository_url) <= 2048));

drop function public.founder_upsert_project(uuid,text,text,text,text,text,text[],text);

create function public.founder_upsert_project(
  p_project_id uuid,
  p_slug text,
  p_owner_category text,
  p_title text,
  p_summary text,
  p_stage text,
  p_tags text[],
  p_visibility text,
  p_website_url text,
  p_repository_url text
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare result_id uuid;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_slug is null or p_slug !~ '^[a-z0-9]+([a-z0-9-]*[a-z0-9])?$'
    or p_owner_category is null or p_owner_category not in ('official', 'community', 'partner')
    or p_title is null or length(btrim(p_title)) not between 1 and 160
    or p_summary is null or length(p_summary) > 2000
    or p_stage is null or p_stage not in ('idea', 'prototype', 'alpha', 'beta', 'released', 'in_development', 'paused', 'archived')
    or p_visibility is null or p_visibility not in ('public', 'members', 'private')
    or p_tags is null or cardinality(p_tags) > 32
    or exists (select 1 from unnest(p_tags) tag where length(tag) not between 1 and 48)
    or (p_website_url is not null and (p_website_url !~ '^https://[^/[:space:]]+(/.*)?$' or length(p_website_url) > 2048))
    or (p_repository_url is not null and (p_repository_url !~ '^https://[^/[:space:]]+(/.*)?$' or length(p_repository_url) > 2048)) then
    raise exception 'Invalid project fields' using errcode = '22023';
  end if;
  if p_project_id is null then
    insert into public.projects(slug, owner_category, title, summary, stage, tags, visibility, website_url, repository_url)
    values (p_slug, p_owner_category, btrim(p_title), p_summary, p_stage, p_tags, p_visibility, p_website_url, p_repository_url)
    returning id into result_id;
  else
    update public.projects set slug=p_slug, owner_category=p_owner_category, title=btrim(p_title),
      summary=p_summary, stage=p_stage, tags=p_tags, visibility=p_visibility,
      website_url=p_website_url, repository_url=p_repository_url
    where id=p_project_id returning id into result_id;
    if result_id is null then raise exception 'Project unavailable' using errcode = '22023'; end if;
  end if;
  insert into public.admin_audit_log(actor_id, action, target_type, target_id, summary)
  values ((select auth.uid()), case when p_project_id is null then 'project_created' else 'project_updated' end,
    'project', result_id, case when p_project_id is null then 'Project created: ' else 'Project updated: ' end || p_slug);
  return jsonb_build_object('id', result_id, 'slug', p_slug);
end;
$$;

create or replace function public.list_projects(
  p_cursor text default null,
  p_limit integer default 20,
  p_stage text default null,
  p_owner_category text default null,
  p_tag text default null
)
returns jsonb
language plpgsql stable security invoker set search_path = '' as $$
declare
  page_size integer := greatest(1, least(coalesce(p_limit, 20), 50));
  cursor_data jsonb;
  cursor_stage_order integer;
  cursor_title text;
  cursor_id uuid;
  page_items jsonb;
  has_more boolean;
  next_cursor text;
begin
  if p_cursor is not null then
    if length(p_cursor) > 512 or p_cursor !~ '^[A-Za-z0-9_-]+$' then
      raise exception 'Invalid project cursor' using errcode = '22023';
    end if;
    begin
      cursor_data := convert_from(
        decode(rpad(translate(p_cursor, '-_', '+/'), length(p_cursor) + mod(4 - mod(length(p_cursor), 4), 4), '='), 'base64'),
        'UTF8'
      )::jsonb;
      cursor_stage_order := (cursor_data ->> 'stage_order')::integer;
      cursor_title := cursor_data ->> 'title';
      cursor_id := (cursor_data ->> 'id')::uuid;
      if cursor_stage_order is null or cursor_title is null or cursor_id is null then
        raise exception 'Invalid project cursor' using errcode = '22023';
      end if;
    exception when others then
      raise exception 'Invalid project cursor' using errcode = '22023';
    end;
  end if;

  with selected as materialized (
    select p.id, p.slug, p.owner_category, p.title, p.summary, p.stage, p.tags,
      p.visibility, p.website_url, p.repository_url, p.status_as_of, p.created_at, p.updated_at,
      case p.stage when 'released' then 0 when 'beta' then 1 when 'in_development' then 2
        when 'paused' then 3 when 'archived' then 4 else 5 end as stage_order,
      row_number() over (order by
        case p.stage when 'released' then 0 when 'beta' then 1 when 'in_development' then 2
          when 'paused' then 3 when 'archived' then 4 else 5 end,
        lower(p.title) collate "C", p.id) as row_number
    from public.projects p
    where (p_stage is null or p.stage = p_stage)
      and (p_owner_category is null or p.owner_category = p_owner_category)
      and (p_tag is null or p.tags @> array[p_tag])
      and (cursor_stage_order is null or
        case p.stage when 'released' then 0 when 'beta' then 1 when 'in_development' then 2
          when 'paused' then 3 when 'archived' then 4 else 5 end > cursor_stage_order
        or (case p.stage when 'released' then 0 when 'beta' then 1 when 'in_development' then 2
          when 'paused' then 3 when 'archived' then 4 else 5 end = cursor_stage_order
          and (lower(p.title) collate "C" > cursor_title collate "C"
            or (lower(p.title) collate "C" = cursor_title collate "C" and p.id > cursor_id))))
    order by stage_order, lower(p.title) collate "C", p.id
    limit page_size + 1
  ), page as (
    select * from selected where row_number <= page_size
  )
  select
    coalesce(jsonb_agg(to_jsonb(page) - 'row_number' - 'stage_order' order by page.row_number), '[]'::jsonb),
    exists(select 1 from selected where row_number > page_size),
    (select stage_order from page order by row_number desc limit 1),
    (select lower(title) from page order by row_number desc limit 1),
    (select id from page order by row_number desc limit 1)
  into page_items, has_more, cursor_stage_order, cursor_title, cursor_id
  from page;

  if has_more then
    next_cursor := rtrim(replace(replace(replace(
      encode(convert_to(jsonb_build_object('stage_order', cursor_stage_order, 'title', cursor_title, 'id', cursor_id)::text, 'UTF8'), 'base64'),
      E'\n', ''), '+', '-'), '/', '_'), '=');
  end if;
  return jsonb_build_object('items', page_items, 'next_cursor', next_cursor);
end;
$$;

revoke all on function public.founder_upsert_project(uuid,text,text,text,text,text,text[],text,text,text) from public, anon, service_role;
grant execute on function public.founder_upsert_project(uuid,text,text,text,text,text,text[],text,text,text) to authenticated;
revoke all on function public.list_projects(text,integer,text,text,text) from public;
grant execute on function public.list_projects(text,integer,text,text,text) to anon, authenticated;
