create function public.founder_list_members(p_cursor text default null, p_limit integer default 20)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  page_size integer := greatest(1, least(coalesce(p_limit, 20), 50));
  cursor_data jsonb;
  cursor_created_at timestamptz;
  cursor_id uuid;
  page_items jsonb;
  has_more boolean;
  next_cursor text;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_cursor is not null then
    if length(p_cursor) > 512 or p_cursor !~ '^[A-Za-z0-9_-]+$' then
      raise exception 'Invalid member cursor' using errcode = '22023';
    end if;
    begin
      cursor_data := convert_from(
        decode(rpad(translate(p_cursor, '-_', '+/'), length(p_cursor) + mod(4 - mod(length(p_cursor), 4), 4), '='), 'base64'),
        'UTF8'
      )::jsonb;
      cursor_created_at := (cursor_data ->> 'created_at')::timestamptz;
      cursor_id := (cursor_data ->> 'id')::uuid;
      if cursor_created_at is null or cursor_id is null then
        raise exception 'Invalid member cursor' using errcode = '22023';
      end if;
    exception when others then
      raise exception 'Invalid member cursor' using errcode = '22023';
    end;
  end if;

  with selected as materialized (
    select p.id, p.handle, p.display_name, p.visibility, p.account_state, p.created_at,
      coalesce((select jsonb_agg(pr.role order by pr.role) from public.platform_roles pr where pr.user_id = p.id), '[]'::jsonb) as roles,
      row_number() over (order by p.created_at desc, p.id desc) as row_number
    from public.profiles p
    where cursor_created_at is null or p.created_at < cursor_created_at
      or (p.created_at = cursor_created_at and p.id < cursor_id)
    order by p.created_at desc, p.id desc
    limit page_size + 1
  ), page as (
    select * from selected where row_number <= page_size
  )
  select coalesce(jsonb_agg(to_jsonb(page) - 'row_number' order by page.row_number), '[]'::jsonb),
    exists(select 1 from selected where row_number > page_size),
    (select id from page order by row_number desc limit 1),
    (select created_at from page order by row_number desc limit 1)
  into page_items, has_more, cursor_id, cursor_created_at
  from page;

  if has_more then
    next_cursor := rtrim(replace(replace(replace(
      encode(convert_to(jsonb_build_object('created_at', cursor_created_at, 'id', cursor_id)::text, 'UTF8'), 'base64'),
      E'\n', ''), '+', '-'), '/', '_'), '=');
  end if;
  return jsonb_build_object('items', page_items, 'next_cursor', next_cursor);
end;
$$;

create function public.founder_list_audit(p_cursor text default null, p_limit integer default 20)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  page_size integer := greatest(1, least(coalesce(p_limit, 20), 50));
  cursor_data jsonb;
  cursor_created_at timestamptz;
  cursor_id uuid;
  page_items jsonb;
  has_more boolean;
  next_cursor text;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_cursor is not null then
    if length(p_cursor) > 512 or p_cursor !~ '^[A-Za-z0-9_-]+$' then
      raise exception 'Invalid audit cursor' using errcode = '22023';
    end if;
    begin
      cursor_data := convert_from(
        decode(rpad(translate(p_cursor, '-_', '+/'), length(p_cursor) + mod(4 - mod(length(p_cursor), 4), 4), '='), 'base64'),
        'UTF8'
      )::jsonb;
      cursor_created_at := (cursor_data ->> 'created_at')::timestamptz;
      cursor_id := (cursor_data ->> 'id')::uuid;
      if cursor_created_at is null or cursor_id is null then
        raise exception 'Invalid audit cursor' using errcode = '22023';
      end if;
    exception when others then
      raise exception 'Invalid audit cursor' using errcode = '22023';
    end;
  end if;

  with selected as materialized (
    select a.id, a.actor_id, a.action, a.target_type, a.target_id, a.summary, a.created_at,
      row_number() over (order by a.created_at desc, a.id desc) as row_number
    from public.admin_audit_log a
    where cursor_created_at is null or a.created_at < cursor_created_at
      or (a.created_at = cursor_created_at and a.id < cursor_id)
    order by a.created_at desc, a.id desc
    limit page_size + 1
  ), page as (
    select * from selected where row_number <= page_size
  )
  select coalesce(jsonb_agg(to_jsonb(page) - 'row_number' order by page.row_number), '[]'::jsonb),
    exists(select 1 from selected where row_number > page_size),
    (select id from page order by row_number desc limit 1),
    (select created_at from page order by row_number desc limit 1)
  into page_items, has_more, cursor_id, cursor_created_at
  from page;

  if has_more then
    next_cursor := rtrim(replace(replace(replace(
      encode(convert_to(jsonb_build_object('created_at', cursor_created_at, 'id', cursor_id)::text, 'UTF8'), 'base64'),
      E'\n', ''), '+', '-'), '/', '_'), '=');
  end if;
  return jsonb_build_object('items', page_items, 'next_cursor', next_cursor);
end;
$$;

revoke all on function public.founder_list_members(text, integer) from public, anon, service_role;
revoke all on function public.founder_list_audit(text, integer) from public, anon, service_role;
grant execute on function public.founder_list_members(text, integer) to authenticated;
grant execute on function public.founder_list_audit(text, integer) to authenticated;
