alter table public.platform_roles
  drop constraint platform_roles_role_check;
alter table public.platform_roles
  add constraint platform_roles_role_check
  check (role in ('administrator', 'moderator', 'founder'));

create unique index platform_roles_one_founder
  on public.platform_roles (role)
  where role = 'founder';

create function public.viewer_is_founder()
returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.platform_roles pr
    where pr.user_id = (select auth.uid()) and pr.role = 'founder'
  );
$$;
revoke all on function public.viewer_is_founder() from public, anon;
grant execute on function public.viewer_is_founder() to authenticated;

create policy profiles_read_founder on public.profiles for select to authenticated
using ((select public.viewer_is_founder()));
create policy platform_roles_read_founder on public.platform_roles for select to authenticated
using ((select public.viewer_is_founder()));
create policy projects_read_founder on public.projects for select to authenticated
using ((select public.viewer_is_founder()));
create policy memberships_read_founder on public.project_memberships for select to authenticated
using ((select public.viewer_is_founder()));
