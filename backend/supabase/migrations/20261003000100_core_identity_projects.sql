create extension if not exists pgcrypto with schema extensions;

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  handle text not null unique check (handle = lower(handle) and handle ~ '^[a-z0-9][a-z0-9_-]{2,47}$'),
  display_name text,
  bio text,
  interests text[] not null default '{}',
  visibility text not null default 'members' check (visibility in ('members', 'private')),
  account_state text not null default 'pending' check (account_state in ('pending', 'active', 'suspended', 'closed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.platform_roles (
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('administrator', 'moderator')),
  granted_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  primary key (user_id, role)
);

create table public.projects (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique check (slug = lower(slug) and slug ~ '^[a-z0-9]+([a-z0-9-]*[a-z0-9])?$'),
  owner_category text not null check (owner_category in ('official', 'community', 'partner')),
  title text not null check (length(btrim(title)) between 1 and 160),
  summary text not null default '' check (length(summary) <= 2000),
  stage text not null check (stage in ('idea', 'prototype', 'alpha', 'beta', 'released', 'paused', 'archived')),
  tags text[] not null default '{}',
  visibility text not null default 'public' check (visibility in ('public', 'members', 'private')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.project_memberships (
  project_id uuid not null references public.projects(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('owner', 'maintainer', 'contributor', 'tester')),
  created_at timestamptz not null default now(),
  primary key (project_id, user_id)
);

create table public.project_follows (
  project_id uuid not null references public.projects(id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (project_id, user_id)
);

create table public.invitations (
  id uuid primary key default gen_random_uuid(),
  normalized_email text not null check (normalized_email = lower(btrim(normalized_email))),
  invited_by uuid not null references auth.users(id) on delete restrict,
  expires_at timestamptz not null,
  accepted_at timestamptz,
  auth_user_id uuid unique references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  check ((accepted_at is null) = (auth_user_id is null))
);

create unique index invitations_one_pending_email
  on public.invitations(normalized_email) where accepted_at is null;
create index projects_public_catalogue_order on public.projects(updated_at desc, id asc) where visibility = 'public';
create index project_memberships_by_member on public.project_memberships(user_id, project_id);
create index project_follows_by_member on public.project_follows(user_id, created_at desc, project_id);

create function public.set_updated_at()
returns trigger language plpgsql set search_path = '' as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create trigger profiles_set_updated_at before update on public.profiles
for each row execute function public.set_updated_at();
create trigger projects_set_updated_at before update on public.projects
for each row execute function public.set_updated_at();

create function public.viewer_is_active_member()
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.profiles where id = auth.uid() and account_state = 'active')
$$;
revoke all on function public.viewer_is_active_member() from public, anon;
grant execute on function public.viewer_is_active_member() to authenticated;

create function public.create_pending_profile_for_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  generated_handle text;
begin
  generated_handle := 'member-' || replace(new.id::text, '-', '');
  insert into public.profiles (id, handle, display_name, account_state)
  values (new.id, generated_handle, null, 'pending')
  on conflict (id) do nothing;
  return new;
end;
$$;

create function public.accept_invited_auth_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if old.email_confirmed_at is null and new.email_confirmed_at is not null and new.email is not null then
    update public.invitations
    set accepted_at = now(), auth_user_id = new.id
    where normalized_email = lower(btrim(new.email))
      and accepted_at is null
      and expires_at > now();

    if found then
      update public.profiles set account_state = 'active' where id = new.id and account_state = 'pending';
    end if;
  end if;
  return new;
end;
$$;

create trigger auth_user_profile_after_insert after insert on auth.users
for each row execute function public.create_pending_profile_for_auth_user();
create trigger auth_user_invitation_after_confirmation after update of email_confirmed_at on auth.users
for each row execute function public.accept_invited_auth_user();

revoke all on function public.create_pending_profile_for_auth_user() from public, anon, authenticated;
revoke all on function public.accept_invited_auth_user() from public, anon, authenticated;

alter table public.profiles enable row level security;
alter table public.platform_roles enable row level security;
alter table public.projects enable row level security;
alter table public.project_memberships enable row level security;
alter table public.project_follows enable row level security;
alter table public.invitations enable row level security;

create policy profiles_read_visible on public.profiles for select to authenticated
using (
  id = (select auth.uid())
  or (visibility = 'members' and account_state = 'active'
      and (select public.viewer_is_active_member()))
);
create policy profiles_update_self on public.profiles for update to authenticated
using (id = (select auth.uid()))
with check (id = (select auth.uid()));

create policy platform_roles_read_self on public.platform_roles for select to authenticated
using (user_id = (select auth.uid()));

create policy projects_read_public on public.projects for select to anon, authenticated
using (visibility = 'public');
create policy projects_read_member_scope on public.projects for select to authenticated
using (
  visibility = 'members' and (select public.viewer_is_active_member())
);
create policy projects_read_private_member on public.projects for select to authenticated
using (
  visibility = 'private' and exists (
    select 1 from public.project_memberships pm where pm.project_id = projects.id and pm.user_id = (select auth.uid())
  )
);

create policy memberships_read_self on public.project_memberships for select to authenticated
using (user_id = (select auth.uid()));

create policy follows_read_self on public.project_follows for select to authenticated
using (user_id = (select auth.uid()));
create policy follows_insert_self on public.project_follows for insert to authenticated
with check (
  user_id = (select auth.uid())
  and exists (select 1 from public.profiles p where p.id = (select auth.uid()) and p.account_state = 'active')
  and exists (select 1 from public.projects p where p.id = project_follows.project_id and p.visibility = 'public')
);
create policy follows_delete_self on public.project_follows for delete to authenticated
using (user_id = (select auth.uid()));

revoke all on public.profiles, public.platform_roles, public.projects,
  public.project_memberships, public.project_follows, public.invitations
  from anon, authenticated;
grant select on public.projects to anon, authenticated;
grant select on public.profiles, public.platform_roles, public.project_memberships,
  public.project_follows to authenticated;
grant insert, delete on public.project_follows to authenticated;
grant update (display_name, bio, interests, visibility) on public.profiles to authenticated;
grant select, insert, update, delete on public.invitations to service_role;

create function public.list_projects(
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
  cursor_updated_at timestamptz;
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
        decode(
          rpad(translate(p_cursor, '-_', '+/'), length(p_cursor) + mod(4 - mod(length(p_cursor), 4), 4), '='),
          'base64'
        ),
        'UTF8'
      )::jsonb;
      cursor_updated_at := (cursor_data ->> 'updated_at')::timestamptz;
      cursor_id := (cursor_data ->> 'id')::uuid;
      if cursor_updated_at is null or cursor_id is null then
        raise exception 'Invalid project cursor' using errcode = '22023';
      end if;
    exception when others then
      raise exception 'Invalid project cursor' using errcode = '22023';
    end;
  end if;

  with selected as materialized (
    select p.id, p.slug, p.owner_category, p.title, p.summary, p.stage, p.tags,
      p.visibility, p.created_at, p.updated_at,
      row_number() over (order by p.updated_at desc, p.id asc) as row_number
    from public.projects p
    where (p_stage is null or p.stage = p_stage)
      and (p_owner_category is null or p.owner_category = p_owner_category)
      and (p_tag is null or p.tags @> array[p_tag])
      and (cursor_updated_at is null or p.updated_at < cursor_updated_at
        or (p.updated_at = cursor_updated_at and p.id > cursor_id))
    order by p.updated_at desc, p.id asc
    limit page_size + 1
  ), page as (
    select * from selected where row_number <= page_size
  )
  select
    coalesce(jsonb_agg(to_jsonb(page) - 'row_number' order by page.row_number), '[]'::jsonb),
    exists(select 1 from selected where row_number > page_size),
    (select id from page order by row_number desc limit 1),
    (select updated_at from page order by row_number desc limit 1)
  into page_items, has_more, cursor_id, cursor_updated_at
  from page;

  if has_more then
    next_cursor := rtrim(
      replace(replace(replace(
        encode(convert_to(jsonb_build_object('updated_at', cursor_updated_at, 'id', cursor_id)::text, 'UTF8'), 'base64'),
        E'\n', ''), '+', '-'), '/', '_'),
      '='
    );
  end if;

  return jsonb_build_object('items', page_items, 'next_cursor', next_cursor);
end;
$$;

revoke all on function public.list_projects(text, integer, text, text, text) from public;
grant execute on function public.list_projects(text, integer, text, text, text) to anon, authenticated;

revoke all on function public.set_updated_at() from public, anon, authenticated;
