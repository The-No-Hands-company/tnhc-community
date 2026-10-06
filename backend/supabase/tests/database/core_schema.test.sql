begin;
select plan(63);

select has_table('public', 'profiles', 'profiles table exists');
select has_table('public', 'platform_roles', 'platform roles table exists');
select has_table('public', 'projects', 'projects table exists');
select has_table('public', 'project_memberships', 'project memberships table exists');
select has_table('public', 'project_follows', 'project follows table exists');
select has_table('public', 'invitations', 'invitations table exists');

select ok((select relrowsecurity from pg_class where oid = to_regclass('public.profiles')), 'profiles has RLS enabled');
select ok((select relrowsecurity from pg_class where oid = to_regclass('public.platform_roles')), 'platform_roles has RLS enabled');
select ok((select relrowsecurity from pg_class where oid = to_regclass('public.projects')), 'projects has RLS enabled');
select ok((select relrowsecurity from pg_class where oid = to_regclass('public.project_memberships')), 'project_memberships has RLS enabled');
select ok((select relrowsecurity from pg_class where oid = to_regclass('public.project_follows')), 'project_follows has RLS enabled');
select ok((select relrowsecurity from pg_class where oid = to_regclass('public.invitations')), 'invitations has RLS enabled');

select ok((select count(*) = 9 from pg_attribute where attrelid = to_regclass('public.profiles') and attname = any(array['id','handle','display_name','bio','interests','visibility','account_state','created_at','updated_at']) and not attisdropped), 'profiles has the documented columns');

select ok(exists(select 1 from pg_constraint where conrelid=to_regclass('public.profiles') and contype='u' and conkey=array[(select attnum from pg_attribute where attrelid=to_regclass('public.profiles') and attname='handle')]::smallint[]), 'profile handles are unique');
select ok(exists(select 1 from pg_constraint where conrelid=to_regclass('public.projects') and contype='u' and conkey=array[(select attnum from pg_attribute where attrelid=to_regclass('public.projects') and attname='slug')]::smallint[]), 'project slugs are unique');
select ok(exists(select 1 from pg_constraint where conrelid=to_regclass('public.project_follows') and contype in ('u','p') and cardinality(conkey)=2), 'project follows enforce pair uniqueness');
select ok((select column_default like '%auth.uid%' from information_schema.columns where table_schema='public' and table_name='project_follows' and column_name='user_id'), 'follow owner defaults to the authenticated user');
select ok((select count(*) >= 2 from pg_constraint where conrelid=to_regclass('public.projects') and contype='c'), 'project owner category and stage have checks');
select ok(exists(select 1 from pg_constraint where conrelid=to_regclass('public.profiles') and contype='f' and confrelid='auth.users'::regclass), 'profiles reference Auth users');
select ok(exists(select 1 from pg_constraint where conrelid=to_regclass('public.project_memberships') and contype='f' and confrelid=to_regclass('public.projects')), 'memberships reference projects');
select ok(exists(select 1 from pg_constraint where conrelid=to_regclass('public.project_memberships') and contype='f' and confrelid='auth.users'::regclass), 'memberships reference Auth users');
select ok((select count(*) >= 6 from pg_policies where schemaname='public'), 'RLS policies are explicitly defined');
select ok(has_table_privilege('service_role', 'public.invitations', 'SELECT'), 'invitation function can check pending invitations');
select ok(has_table_privilege('service_role', 'public.invitations', 'INSERT'), 'invitation function can create invitation records');
select ok(has_table_privilege('service_role', 'public.invitations', 'UPDATE'), 'Auth confirmation can consume invitations');
select ok(has_table_privilege('service_role', 'public.invitations', 'DELETE'), 'invitation function can expire and clean up invitations');
select ok(to_regprocedure('public.list_projects(text,integer,text,text,text)') is not null, 'projects expose the documented cursor RPC');

insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at)
values
  ('10000000-0000-0000-0000-000000000001', 'authenticated', 'authenticated', 'alpha@example.test', '', now()),
  ('10000000-0000-0000-0000-000000000002', 'authenticated', 'authenticated', 'beta@example.test', '', now()),
  ('10000000-0000-0000-0000-000000000003', 'authenticated', 'authenticated', 'gamma@example.test', '', now());
select is((select account_state from public.profiles where id='10000000-0000-0000-0000-000000000001'), 'pending', 'Auth user receives a pending profile');
select is((select handle from public.profiles where id='10000000-0000-0000-0000-000000000001'), 'member-10000000000000000000000000000001', 'initial handle is server-generated from the Auth UUID');
insert into auth.users (id, aud, role, email, encrypted_password)
values
  ('10000000-0000-0000-0000-000000000004', 'authenticated', 'authenticated', 'delta@example.test', ''),
  ('10000000-0000-0000-0000-000000000005', 'authenticated', 'authenticated', 'epsilon@example.test', '');
insert into public.invitations (normalized_email, invited_by, expires_at)
values
  ('delta@example.test', '10000000-0000-0000-0000-000000000001', now() + interval '1 day'),
  ('epsilon@example.test', '10000000-0000-0000-0000-000000000001', now() - interval '1 day');
update auth.users set email_confirmed_at = now() where id='10000000-0000-0000-0000-000000000004';
update auth.users set email_confirmed_at = now() where id='10000000-0000-0000-0000-000000000005';
select is((select account_state from public.profiles where id='10000000-0000-0000-0000-000000000004'), 'active', 'valid invitation activates its confirmed account');
select is((select auth_user_id from public.invitations where normalized_email='delta@example.test'), '10000000-0000-0000-0000-000000000004'::uuid, 'confirmation accepts and consumes the invitation');
select is((select account_state from public.profiles where id='10000000-0000-0000-0000-000000000005'), 'pending', 'expired invitation does not activate the account');
select is((select accepted_at from public.invitations where normalized_email='epsilon@example.test'), null::timestamptz, 'expired invitation remains unused');
update public.profiles set display_name = 'Alpha', visibility = 'members', account_state = 'active'
where id = '10000000-0000-0000-0000-000000000001';
update public.profiles set display_name = 'Beta', visibility = 'members', account_state = 'active'
where id = '10000000-0000-0000-0000-000000000002';
update public.profiles set display_name = 'Gamma', visibility = 'private', account_state = 'active'
where id = '10000000-0000-0000-0000-000000000003';
insert into public.projects (id, slug, owner_category, title, summary, stage, visibility) values
  ('20000000-0000-0000-0000-000000000001', 'demo-public', 'official', '[DEMO] Public', 'Synthetic policy fixture', 'prototype', 'public'),
  ('20000000-0000-0000-0000-000000000002', 'demo-members', 'official', '[DEMO] Members', 'Synthetic policy fixture', 'prototype', 'members'),
  ('20000000-0000-0000-0000-000000000003', 'demo-private', 'official', '[DEMO] Private', 'Synthetic policy fixture', 'prototype', 'private');
insert into public.project_memberships (project_id, user_id, role) values
  ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'tester');

set local role anon;
select is((select count(*)::integer from public.projects where id in ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003')), 1, 'anonymous visitors see only the public project among the policy fixtures');
select is(jsonb_array_length((public.list_projects(null, 1, null, null, null)->'items')), 1, 'catalogue RPC returns a bounded first page');
select ok((public.list_projects(null, 1, null, null, null)->>'next_cursor') is not null, 'catalogue RPC returns an opaque next cursor');
select is(jsonb_array_length((public.list_projects(public.list_projects(null, 1, null, null, null)->>'next_cursor', 1, null, null, null)->'items')), 1, 'catalogue cursor retrieves the next page');
select throws_ok($$select public.list_projects('broken-cursor', 1, null, null, null)$$, '22023', null, 'malformed catalogue cursor is rejected');
select throws_ok('select count(*) from public.profiles', '42501', null, 'anonymous visitors cannot read profiles');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is((select count(*)::integer from public.projects where id in ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003')), 3, 'active members see public, member and explicitly joined private projects');
select is((select count(*)::integer from public.profiles where id in ('10000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000003')), 2, 'members see member-visible profiles but not private profiles');
select lives_ok($$update public.profiles set display_name = 'Alpha Updated' where id = '10000000-0000-0000-0000-000000000001'$$, 'member can edit own profile');
select lives_ok($test$do $body$ declare changed integer; begin
  update public.profiles set display_name = 'Attempt' where id = '10000000-0000-0000-0000-000000000002';
  get diagnostics changed = row_count;
  if changed <> 0 then raise exception 'cross-member update affected rows'; end if;
end $body$$test$, 'member cannot edit another profile');
select throws_ok($$update public.profiles set account_state = 'suspended' where id = '10000000-0000-0000-0000-000000000001'$$, '42501', null, 'member cannot change account state');
select throws_ok($$update public.projects set title = 'Changed' where id = '20000000-0000-0000-0000-000000000001'$$, '42501', null, 'member cannot edit project catalogue');
select lives_ok($$insert into public.project_follows (project_id) values ('20000000-0000-0000-0000-000000000001')$$, 'member can follow a public project without supplying an owner ID');
select throws_ok($$insert into public.project_follows (project_id, user_id) values ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002')$$, '42501', null, 'member cannot create a follow for another user');
select throws_ok($$insert into public.platform_roles (user_id, role) values ('10000000-0000-0000-0000-000000000001', 'administrator')$$, '42501', null, 'member cannot grant platform roles');
select throws_ok('select count(*) from public.invitations', '42501', null, 'member cannot inspect invitations');
select throws_ok($$insert into public.project_memberships (project_id, user_id, role) values ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'maintainer')$$, '42501', null, 'member cannot grant project roles');
select is((select count(*)::integer from public.project_follows where user_id = '10000000-0000-0000-0000-000000000001'), 1, 'member sees own follows');
select lives_ok($$insert into public.project_follows (project_id) values ('20000000-0000-0000-0000-000000000001') on conflict (project_id, user_id) do nothing$$, 'retrying a follow is idempotent');
select is((select count(*)::integer from public.project_follows where user_id = '10000000-0000-0000-0000-000000000001'), 1, 'retry creates no duplicate follow');
select lives_ok($$update public.profiles set display_name = 'Alpha Updated' where id = '10000000-0000-0000-0000-000000000001'$$, 'retrying a profile update is safe');
select is((select display_name from public.profiles where id = '10000000-0000-0000-0000-000000000001'), 'Alpha Updated', 'profile retry preserves requested value');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000002', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is((select count(*)::integer from public.projects where id in ('20000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000003')), 2, 'other active member cannot read private project without membership');
select is((select count(*)::integer from public.profiles where id in ('10000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000003')), 2, 'private profile remains hidden from another member');
select is((select count(*)::integer from public.project_follows where user_id = '10000000-0000-0000-0000-000000000001'), 0, 'other member cannot read the first member follows');
with removed as (delete from public.project_follows where user_id = '10000000-0000-0000-0000-000000000001' returning *)
select is((select count(*)::integer from removed), 0, 'other member cannot delete the first member follows');
select throws_ok($$insert into public.project_follows (project_id) values ('20000000-0000-0000-0000-000000000003')$$, '42501', null, 'other member cannot follow an inaccessible private project');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
select is((select count(*)::integer from public.project_follows where user_id = '10000000-0000-0000-0000-000000000001'), 1, 'cross-member deletion leaves the original follow intact');
select lives_ok($$delete from public.project_follows where project_id = '20000000-0000-0000-0000-000000000001'$$, 'member can unfollow own project');
with removed as (delete from public.project_follows where project_id = '20000000-0000-0000-0000-000000000001' returning *)
select is((select count(*)::integer from removed), 0, 'retrying unfollow is a no-op');
reset role;

select * from finish();
rollback;
