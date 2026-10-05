create function public.founder_list_content(p_state text default 'hidden', p_cursor text default null, p_limit integer default 20)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  page_size integer := greatest(1, least(coalesce(p_limit,20),50));
  cursor_data jsonb;
  cursor_created_at timestamptz;
  cursor_id uuid;
  cursor_type text;
  page_items jsonb;
  has_more boolean;
  last_created_at timestamptz;
  last_id uuid;
  last_type text;
  next_cursor text;
begin
  if not (select public.viewer_is_founder()) then raise exception 'Founder access required' using errcode='42501'; end if;
  if p_state is null or p_state not in ('visible','hidden','removed','all') then raise exception 'Invalid content state' using errcode='22023'; end if;
  if p_cursor is not null then
    if length(p_cursor)>1024 or p_cursor !~ '^[A-Za-z0-9_-]+$' then raise exception 'Invalid content cursor' using errcode='22023'; end if;
    begin
      cursor_data := convert_from(decode(rpad(translate(p_cursor,'-_','+/'),length(p_cursor)+mod(4-mod(length(p_cursor),4),4),'='),'base64'),'UTF8')::jsonb;
      cursor_created_at := (cursor_data->>'created_at')::timestamptz;
      cursor_id := (cursor_data->>'id')::uuid;
      cursor_type := cursor_data->>'content_type';
      if cursor_created_at is null or cursor_id is null or cursor_type is null or cursor_type not in ('post','comment') then raise exception 'Invalid content cursor'; end if;
    exception when others then raise exception 'Invalid content cursor' using errcode='22023'; end;
  end if;
  with all_content as materialized (
    select 'post'::text as content_type, p.id, p.author_id, coalesce(p.project_id,p.topic_id) as context_id,
      p.body, p.moderation_state, p.created_at
    from public.posts p where p_state='all' or p.moderation_state=p_state
    union all
    select 'comment'::text, c.id, c.author_id, c.post_id, c.body, c.moderation_state, c.created_at
    from public.comments c where p_state='all' or c.moderation_state=p_state
  ), filtered as materialized (
    select *, row_number() over(order by created_at desc,id desc,content_type asc) as row_no
    from all_content
    where cursor_created_at is null or created_at<cursor_created_at
      or (created_at=cursor_created_at and (id<cursor_id or (id=cursor_id and content_type>cursor_type)))
    order by created_at desc,id desc,content_type asc limit page_size+1
  ), page as (select * from filtered where row_no<=page_size)
  select coalesce(jsonb_agg(jsonb_build_object(
      'content_type',content_type,'id',id,'author_id',author_id,'context_id',context_id,
      'body',body,'moderation_state',moderation_state,'created_at',created_at
    ) order by row_no),'[]'::jsonb),
    exists(select 1 from filtered where row_no>page_size),
    (select created_at from page order by row_no desc limit 1),
    (select id from page order by row_no desc limit 1),
    (select content_type from page order by row_no desc limit 1)
  into page_items,has_more,last_created_at,last_id,last_type from page;
  if has_more then
    next_cursor := rtrim(replace(replace(replace(encode(convert_to(jsonb_build_object(
      'created_at',last_created_at,'id',last_id,'content_type',last_type)::text,'UTF8'),'base64'),E'\n',''),'+','-'),'/','_'),'=');
  end if;
  return jsonb_build_object('items',page_items,'next_cursor',next_cursor);
end;
$$;

create function public.founder_set_content_moderation(p_content_type text,p_content_id uuid,p_state text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare affected integer;
begin
  if not (select public.viewer_is_founder()) then raise exception 'Founder access required' using errcode='42501'; end if;
  if p_content_type is null or p_content_type not in ('post','comment') or p_content_id is null or p_state is null or p_state not in ('visible','hidden','removed') then
    raise exception 'Invalid content moderation request' using errcode='22023';
  end if;
  if p_content_type='post' then
    update public.posts set moderation_state=p_state where id=p_content_id and moderation_state<>p_state;
  else
    update public.comments set moderation_state=p_state where id=p_content_id and moderation_state<>p_state;
  end if;
  get diagnostics affected=row_count;
  if affected=0 then
    if p_content_type='post' and not exists(select 1 from public.posts where id=p_content_id) then raise exception 'Content unavailable' using errcode='22023'; end if;
    if p_content_type='comment' and not exists(select 1 from public.comments where id=p_content_id) then raise exception 'Content unavailable' using errcode='22023'; end if;
  else
    insert into public.admin_audit_log(actor_id,action,target_type,target_id,summary)
    values((select auth.uid()),'content_moderation_changed',p_content_type,p_content_id,'Content state changed to '||p_state);
  end if;
  return jsonb_build_object('updated',affected=1,'content_type',p_content_type,'id',p_content_id,'moderation_state',p_state);
end;
$$;

revoke all on function public.founder_list_content(text,text,integer) from public,anon,service_role;
revoke all on function public.founder_set_content_moderation(text,uuid,text) from public,anon,service_role;
grant execute on function public.founder_list_content(text,text,integer) to authenticated;
grant execute on function public.founder_set_content_moderation(text,uuid,text) to authenticated;
