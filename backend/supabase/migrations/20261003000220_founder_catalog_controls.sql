create policy topics_read_founder on public.topics for select to authenticated
using ((select public.viewer_is_founder()));

create policy project_memberships_read_founder on public.project_memberships for select to authenticated
using ((select public.viewer_is_founder()));
create policy topic_memberships_read_founder on public.topic_memberships for select to authenticated
using ((select public.viewer_is_founder()));

create function public.founder_list_topics(p_cursor text default null,p_limit integer default 20)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  page_size integer := greatest(1,least(coalesce(p_limit,20),50));
  cursor_data jsonb;
  cursor_created_at timestamptz;
  cursor_id uuid;
  page_items jsonb;
  has_more boolean;
  last_created_at timestamptz;
  last_id uuid;
  next_cursor text;
begin
  if not (select public.viewer_is_founder()) then raise exception 'Founder access required' using errcode='42501'; end if;
  if p_cursor is not null then
    if length(p_cursor)>512 or p_cursor !~ '^[A-Za-z0-9_-]+$' then raise exception 'Invalid topic cursor' using errcode='22023'; end if;
    begin
      cursor_data := convert_from(decode(rpad(translate(p_cursor,'-_','+/'),length(p_cursor)+mod(4-mod(length(p_cursor),4),4),'='),'base64'),'UTF8')::jsonb;
      cursor_created_at := (cursor_data->>'created_at')::timestamptz;
      cursor_id := (cursor_data->>'id')::uuid;
      if cursor_created_at is null or cursor_id is null then raise exception 'Invalid topic cursor'; end if;
    exception when others then raise exception 'Invalid topic cursor' using errcode='22023'; end;
  end if;
  with selected as materialized (
    select t.id,t.slug,t.title,t.description,t.visibility,t.created_at,
      row_number() over(order by t.created_at desc,t.id desc) as row_no
    from public.topics t
    where cursor_created_at is null or t.created_at<cursor_created_at or (t.created_at=cursor_created_at and t.id<cursor_id)
    order by t.created_at desc,t.id desc limit page_size+1
  ), page as (select * from selected where row_no<=page_size)
  select coalesce(jsonb_agg(jsonb_build_object('id',id,'slug',slug,'title',title,'description',description,'visibility',visibility,'created_at',created_at) order by row_no),'[]'::jsonb),
    exists(select 1 from selected where row_no>page_size),
    (select created_at from page order by row_no desc limit 1),
    (select id from page order by row_no desc limit 1)
  into page_items,has_more,last_created_at,last_id from page;
  if has_more then
    next_cursor := rtrim(replace(replace(replace(encode(convert_to(jsonb_build_object('created_at',last_created_at,'id',last_id)::text,'UTF8'),'base64'),E'\n',''),'+','-'),'/','_'),'=');
  end if;
  return jsonb_build_object('items',page_items,'next_cursor',next_cursor);
end;
$$;

create function public.founder_upsert_project(
  p_project_id uuid,
  p_slug text,
  p_owner_category text,
  p_title text,
  p_summary text,
  p_stage text,
  p_tags text[],
  p_visibility text
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
    or p_stage is null or p_stage not in ('idea', 'prototype', 'alpha', 'beta', 'released', 'paused', 'archived')
    or p_visibility is null or p_visibility not in ('public', 'members', 'private')
    or p_tags is null or cardinality(p_tags) > 32
    or exists (select 1 from unnest(p_tags) tag where length(tag) not between 1 and 48) then
    raise exception 'Invalid project fields' using errcode = '22023';
  end if;
  if p_project_id is null then
    insert into public.projects(slug, owner_category, title, summary, stage, tags, visibility)
    values (p_slug, p_owner_category, btrim(p_title), p_summary, p_stage, p_tags, p_visibility)
    returning id into result_id;
  else
    update public.projects set slug=p_slug, owner_category=p_owner_category, title=btrim(p_title),
      summary=p_summary, stage=p_stage, tags=p_tags, visibility=p_visibility
    where id=p_project_id returning id into result_id;
    if result_id is null then raise exception 'Project unavailable' using errcode = '22023'; end if;
  end if;
  insert into public.admin_audit_log(actor_id, action, target_type, target_id, summary)
  values ((select auth.uid()), case when p_project_id is null then 'project_created' else 'project_updated' end,
    'project', result_id, case when p_project_id is null then 'Project created: ' else 'Project updated: ' end || p_slug);
  return jsonb_build_object('id', result_id, 'slug', p_slug);
end;
$$;

create function public.founder_set_project_membership(p_project_id uuid, p_user_id uuid, p_role text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare affected integer;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_project_id is null or p_user_id is null or
    (p_role is not null and p_role not in ('owner', 'maintainer', 'contributor', 'tester')) then
    raise exception 'Invalid project membership request' using errcode = '22023';
  end if;
  if not exists(select 1 from public.projects where id=p_project_id)
    or not exists(select 1 from public.profiles where id=p_user_id and account_state='active') then
    raise exception 'Project or active member unavailable' using errcode = '22023';
  end if;
  if p_role is null then
    delete from public.project_memberships where project_id=p_project_id and user_id=p_user_id;
  else
    insert into public.project_memberships(project_id,user_id,role) values(p_project_id,p_user_id,p_role)
    on conflict(project_id,user_id) do update set role=excluded.role
    where public.project_memberships.role <> excluded.role;
  end if;
  get diagnostics affected = row_count;
  if affected > 0 then
    insert into public.admin_audit_log(actor_id,action,target_type,target_id,summary)
    values((select auth.uid()), case when p_role is null then 'project_member_removed' else 'project_member_changed' end,
      'project', p_project_id, case when p_role is null then 'Project membership removed' else 'Project membership set to '||p_role end);
  end if;
  return jsonb_build_object('updated', affected > 0, 'project_id', p_project_id, 'user_id', p_user_id, 'role', p_role);
end;
$$;

create function public.founder_upsert_topic(
  p_topic_id uuid,
  p_slug text,
  p_title text,
  p_description text,
  p_visibility text
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare result_id uuid;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_slug is null or p_slug !~ '^[a-z0-9]+([a-z0-9-]*[a-z0-9])?$'
    or p_title is null or length(btrim(p_title)) not between 1 and 160
    or p_description is null or length(p_description) > 2000
    or p_visibility is null or p_visibility not in ('public', 'members', 'private') then
    raise exception 'Invalid topic fields' using errcode = '22023';
  end if;
  if p_topic_id is null then
    insert into public.topics(slug,title,description,visibility)
    values(p_slug,btrim(p_title),p_description,p_visibility) returning id into result_id;
  else
    update public.topics set slug=p_slug,title=btrim(p_title),description=p_description,visibility=p_visibility
    where id=p_topic_id returning id into result_id;
    if result_id is null then raise exception 'Topic unavailable' using errcode = '22023'; end if;
  end if;
  insert into public.admin_audit_log(actor_id,action,target_type,target_id,summary)
  values((select auth.uid()),case when p_topic_id is null then 'topic_created' else 'topic_updated' end,
    'topic',result_id,case when p_topic_id is null then 'Topic created: ' else 'Topic updated: ' end||p_slug);
  return jsonb_build_object('id',result_id,'slug',p_slug);
end;
$$;

create function public.founder_set_topic_membership(p_topic_id uuid,p_user_id uuid,p_enabled boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare affected integer;
begin
  if not (select public.viewer_is_founder()) then raise exception 'Founder access required' using errcode='42501'; end if;
  if p_topic_id is null or p_user_id is null or p_enabled is null then raise exception 'Invalid topic membership request' using errcode='22023'; end if;
  if not exists(select 1 from public.topics where id=p_topic_id)
    or not exists(select 1 from public.profiles where id=p_user_id and account_state='active') then
    raise exception 'Topic or active member unavailable' using errcode='22023';
  end if;
  if p_enabled then
    insert into public.topic_memberships(topic_id,user_id) values(p_topic_id,p_user_id) on conflict(topic_id,user_id) do nothing;
  else
    delete from public.topic_memberships where topic_id=p_topic_id and user_id=p_user_id;
  end if;
  get diagnostics affected=row_count;
  if affected>0 then
    insert into public.admin_audit_log(actor_id,action,target_type,target_id,summary)
    values((select auth.uid()),case when p_enabled then 'topic_member_added' else 'topic_member_removed' end,
      'topic',p_topic_id,case when p_enabled then 'Topic membership added' else 'Topic membership removed' end);
  end if;
  return jsonb_build_object('updated',affected>0,'topic_id',p_topic_id,'user_id',p_user_id,'member',p_enabled);
end;
$$;

revoke all on function public.founder_upsert_project(uuid,text,text,text,text,text,text[],text) from public,anon,service_role;
revoke all on function public.founder_list_topics(text,integer) from public,anon,service_role;
revoke all on function public.founder_set_project_membership(uuid,uuid,text) from public,anon,service_role;
revoke all on function public.founder_upsert_topic(uuid,text,text,text,text) from public,anon,service_role;
revoke all on function public.founder_set_topic_membership(uuid,uuid,boolean) from public,anon,service_role;
grant execute on function public.founder_upsert_project(uuid,text,text,text,text,text,text[],text) to authenticated;
grant execute on function public.founder_list_topics(text,integer) to authenticated;
grant execute on function public.founder_set_project_membership(uuid,uuid,text) to authenticated;
grant execute on function public.founder_upsert_topic(uuid,text,text,text,text) to authenticated;
grant execute on function public.founder_set_topic_membership(uuid,uuid,boolean) to authenticated;
