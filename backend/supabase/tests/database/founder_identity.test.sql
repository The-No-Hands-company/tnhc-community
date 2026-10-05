begin;
select no_plan();

select ok(to_regprocedure('public.viewer_is_founder()') is not null, 'Founder role predicate exists');
select ok(exists(select 1 from pg_constraint where conrelid='public.platform_roles'::regclass and contype='c' and pg_get_constraintdef(oid) ilike '%founder%'), 'Founder is a platform role');
select ok(exists(select 1 from pg_indexes where schemaname='public' and indexname='platform_roles_one_founder' and indexdef ilike '%where%founder%'), 'only one Founder assignment can exist');
select ok(not has_function_privilege('anon', 'public.viewer_is_founder()', 'EXECUTE'), 'anonymous callers cannot inspect Founder state');
select ok(not has_table_privilege('authenticated', 'public.platform_roles', 'INSERT'), 'clients cannot insert platform roles directly');
select ok(not has_table_privilege('authenticated', 'public.platform_roles', 'UPDATE'), 'clients cannot update platform roles directly');
select ok(not has_table_privilege('authenticated', 'public.platform_roles', 'DELETE'), 'clients cannot delete platform roles directly');

insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at)
values
 ('50000000-0000-0000-0000-000000000001', 'authenticated', 'authenticated', 'founder@example.test', '', now()),
 ('50000000-0000-0000-0000-000000000002', 'authenticated', 'authenticated', 'ordinary-admin@example.test', '', now()),
 ('50000000-0000-0000-0000-000000000003', 'authenticated', 'authenticated', 'private-project-maintainer@example.test', '', now());
update public.profiles set account_state='active', visibility='private' where id in (
 '50000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002',
 '50000000-0000-0000-0000-000000000003');
insert into public.platform_roles (user_id, role)
values ('50000000-0000-0000-0000-000000000001', 'founder'),
 ('50000000-0000-0000-0000-000000000002', 'administrator');
insert into public.projects (id, slug, owner_category, title, summary, stage, visibility)
values ('50000000-0000-0000-0000-000000000010', 'founder-private', 'official', '[DEMO] Founder visibility', 'Synthetic test fixture', 'prototype', 'private');
insert into public.project_memberships (project_id, user_id, role)
values ('50000000-0000-0000-0000-000000000010', '50000000-0000-0000-0000-000000000003', 'maintainer');

select throws_ok($$insert into public.platform_roles(user_id, role) values ('50000000-0000-0000-0000-000000000002', 'founder')$$, '23505', null, 'a second Founder assignment is rejected');

set local role authenticated;
select set_config('request.jwt.claim.sub', '50000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is(public.viewer_is_founder(), true, 'server role lookup recognizes the Founder account');
select is((select count(*)::integer from public.profiles where id in ('50000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000002')), 2, 'Founder can read private member profiles');
select is((select count(*)::integer from public.projects where id='50000000-0000-0000-0000-000000000010'), 1, 'Founder can read private projects');
select is((select count(*)::integer from public.project_memberships where project_id='50000000-0000-0000-0000-000000000010'), 1, 'Founder can read project memberships');
select is((select count(*)::integer from public.platform_roles where user_id='50000000-0000-0000-0000-000000000002'), 1, 'Founder can read platform role assignments');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '50000000-0000-0000-0000-000000000002', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is(public.viewer_is_founder(), false, 'ordinary administrator is not a Founder');
select is((select count(*)::integer from public.profiles where id='50000000-0000-0000-0000-000000000001'), 0, 'ordinary administrator cannot read another private profile');
select is((select count(*)::integer from public.projects where id='50000000-0000-0000-0000-000000000010'), 0, 'ordinary administrator cannot read an unrelated private project');
select throws_ok($$insert into public.platform_roles(user_id, role) values ('50000000-0000-0000-0000-000000000002', 'moderator')$$, '42501', null, 'ordinary administrator cannot grant platform roles');
select throws_ok($$insert into public.platform_roles(user_id, role) values ('50000000-0000-0000-0000-000000000001', 'founder')$$, '42501', null, 'ordinary administrator cannot grant Founder');
select throws_ok($$delete from public.platform_roles where user_id='50000000-0000-0000-0000-000000000001' and role='founder'$$, '42501', null, 'ordinary administrator cannot remove Founder');
reset role;

set local role anon;
select throws_ok('select public.viewer_is_founder()', '42501', null, 'anonymous request cannot call Founder role lookup');
reset role;

select * from finish();
rollback;
