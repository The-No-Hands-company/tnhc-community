create table public.admin_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid not null references auth.users(id) on delete restrict,
  action text not null check (action ~ '^[a-z][a-z0-9_]{1,63}$'),
  target_type text not null check (target_type ~ '^[a-z][a-z0-9_]{1,63}$'),
  target_id uuid,
  summary text not null check (length(btrim(summary)) between 1 and 240),
  created_at timestamptz not null default now()
);
create index admin_audit_log_by_time on public.admin_audit_log(created_at desc, id desc);
create index admin_audit_log_by_target on public.admin_audit_log(target_type, target_id, created_at desc);

create table public.app_settings (
  key text primary key check (key in ('maintenance_notice', 'feature_flags')),
  value jsonb not null,
  updated_by uuid not null references auth.users(id) on delete restrict,
  updated_at timestamptz not null default now()
);

alter table public.admin_audit_log enable row level security;
alter table public.app_settings enable row level security;
create policy admin_audit_read_founder on public.admin_audit_log for select to authenticated
using ((select public.viewer_is_founder()));
create policy app_settings_read_all on public.app_settings for select to anon, authenticated
using (true);

revoke all on public.admin_audit_log, public.app_settings from anon, authenticated, service_role;
grant select on public.admin_audit_log to authenticated;
grant select on public.app_settings to anon, authenticated;
grant insert on public.admin_audit_log to service_role;

create function public.reject_audit_mutation()
returns trigger language plpgsql set search_path = '' as $$
begin
  raise exception 'Audit records are append-only' using errcode = '42501';
end;
$$;
create trigger admin_audit_log_append_only before update or delete on public.admin_audit_log
for each row execute function public.reject_audit_mutation();
revoke all on function public.reject_audit_mutation() from public, anon, authenticated, service_role;

create function public.founder_set_member_state(p_user_id uuid, p_state text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare affected integer;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_user_id is null or p_state is null or p_state not in ('active', 'suspended', 'closed') then
    raise exception 'Invalid member state request' using errcode = '22023';
  end if;
  if p_user_id = (select auth.uid()) and p_state <> 'active' then
    raise exception 'Founder cannot suspend or close the current account' using errcode = '22023';
  end if;

  update public.profiles set account_state = p_state
  where id = p_user_id and account_state <> p_state;
  get diagnostics affected = row_count;
  if affected = 0 and not exists (select 1 from public.profiles where id = p_user_id) then
    raise exception 'Member unavailable' using errcode = '22023';
  end if;
  if affected = 1 then
    insert into public.admin_audit_log(actor_id, action, target_type, target_id, summary)
    values ((select auth.uid()), 'member_state_changed', 'member', p_user_id, 'Account state changed to ' || p_state);
  end if;
  return jsonb_build_object('updated', affected = 1, 'user_id', p_user_id, 'account_state', p_state);
end;
$$;

create function public.founder_set_platform_role(p_user_id uuid, p_role text, p_enabled boolean)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare affected integer;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_user_id is null or p_role is null or p_role not in ('administrator', 'moderator') or p_enabled is null then
    raise exception 'Invalid platform role request' using errcode = '22023';
  end if;
  if p_user_id = (select auth.uid()) and p_role = 'administrator' and not p_enabled then
    raise exception 'Founder cannot remove their own administration access' using errcode = '22023';
  end if;
  if not exists (select 1 from public.profiles where id = p_user_id and account_state = 'active') then
    raise exception 'Active member unavailable' using errcode = '22023';
  end if;

  if p_enabled then
    insert into public.platform_roles(user_id, role)
    values (p_user_id, p_role) on conflict (user_id, role) do nothing;
    get diagnostics affected = row_count;
  else
    delete from public.platform_roles where user_id = p_user_id and role = p_role;
    get diagnostics affected = row_count;
  end if;
  if affected = 1 then
    insert into public.admin_audit_log(actor_id, action, target_type, target_id, summary)
    values ((select auth.uid()), case when p_enabled then 'platform_role_granted' else 'platform_role_revoked' end,
      'member', p_user_id, case when p_enabled then 'Granted ' else 'Revoked ' end || p_role || ' role');
  end if;
  return jsonb_build_object('updated', affected = 1, 'user_id', p_user_id, 'role', p_role, 'enabled', p_enabled);
end;
$$;

create function public.founder_set_app_setting(p_key text, p_value jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare setting_updated_at timestamptz;
begin
  if not (select public.viewer_is_founder()) then
    raise exception 'Founder access required' using errcode = '42501';
  end if;
  if p_value is null then
    raise exception 'Application setting value is required' using errcode = '22023';
  elsif p_key = 'maintenance_notice' then
    if jsonb_typeof(p_value) <> 'string' or length(btrim(p_value #>> '{}')) > 240 then
      raise exception 'Maintenance notice must be a string up to 240 characters' using errcode = '22023';
    end if;
  elsif p_key = 'feature_flags' then
    if jsonb_typeof(p_value) <> 'object' or (select count(*) from jsonb_object_keys(p_value)) > 64
      or exists (
        select 1 from jsonb_each(p_value) as flag(key, value)
        where key !~ '^[a-z][a-z0-9_]{0,63}$' or jsonb_typeof(value) <> 'boolean'
      ) then
      raise exception 'Feature flags must be a bounded object of boolean values' using errcode = '22023';
    end if;
  else
    raise exception 'Application setting is not allow-listed' using errcode = '22023';
  end if;

  insert into public.app_settings(key, value, updated_by, updated_at)
  values (p_key, p_value, (select auth.uid()), now())
  on conflict (key) do update set value = excluded.value, updated_by = excluded.updated_by, updated_at = excluded.updated_at
  returning updated_at into setting_updated_at;
  insert into public.admin_audit_log(actor_id, action, target_type, summary)
  values ((select auth.uid()), 'app_setting_changed', 'app_setting', p_key || ' changed');
  return jsonb_build_object('key', p_key, 'value', p_value, 'updated_at', setting_updated_at);
end;
$$;

revoke all on function public.founder_set_member_state(uuid, text) from public, anon, service_role;
revoke all on function public.founder_set_platform_role(uuid, text, boolean) from public, anon, service_role;
revoke all on function public.founder_set_app_setting(text, jsonb) from public, anon, service_role;
grant execute on function public.founder_set_member_state(uuid, text) to authenticated;
grant execute on function public.founder_set_platform_role(uuid, text, boolean) to authenticated;
grant execute on function public.founder_set_app_setting(text, jsonb) to authenticated;
