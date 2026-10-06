begin;
select no_plan();

select ok(to_regprocedure('public.founder_list_members(text,integer)') is not null, 'bounded Founder member directory exists');
select ok(to_regprocedure('public.founder_list_audit(text,integer)') is not null, 'bounded Founder audit history exists');

insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at)
values
 ('70000000-0000-0000-0000-000000000001', 'authenticated', 'authenticated', 'directory-founder@example.test', '', now()),
 ('70000000-0000-0000-0000-000000000002', 'authenticated', 'authenticated', 'directory-member-a@example.test', '', now()),
 ('70000000-0000-0000-0000-000000000003', 'authenticated', 'authenticated', 'directory-member-b@example.test', '', now());
update public.profiles set account_state='active' where id in (
 '70000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000003');
delete from public.platform_roles where role = 'founder';
insert into public.platform_roles(user_id, role) values ('70000000-0000-0000-0000-000000000001', 'founder');
insert into public.admin_audit_log(actor_id, action, target_type, summary)
values ('70000000-0000-0000-0000-000000000001', 'directory_fixture', 'test', 'Synthetic test entry.');

set local role authenticated;
select set_config('request.jwt.claim.sub', '70000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is(jsonb_array_length(public.founder_list_members(null, 1)->'items'), 1, 'member directory honors a one-row page');
select ok((public.founder_list_members(null, 1)->>'next_cursor') is not null, 'member directory emits an opaque cursor');
select is(jsonb_array_length(public.founder_list_members(public.founder_list_members(null, 1)->>'next_cursor', 1)->'items'), 1, 'cursor advances across members with equal timestamps');
select is((select count(*)::integer from jsonb_array_elements(public.founder_list_members(null, 51)->'items') as item
  where item->>'id' in ('70000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000003')),
  3, 'member directory returns all fixture members on a bounded page');
select ok(not ((public.founder_list_members(null, 1)->'items'->0) ? 'email'), 'directory does not expose Auth email fields');
select ok(jsonb_array_length(public.founder_list_members(null, 51)->'items') <= 50, 'member directory caps pages at fifty');
select ok(exists (select 1 from jsonb_array_elements(public.founder_list_audit(null, 50)->'items') item
  where item->>'actor_id' = '70000000-0000-0000-0000-000000000001'), 'Founder can read their bounded audit records');
select throws_ok($$select public.founder_list_members('broken-cursor', 1)$$, '22023', null, 'malformed member cursor is rejected safely');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '70000000-0000-0000-0000-000000000002', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select throws_ok($$select public.founder_list_members(null, 1)$$, '42501', null, 'members cannot query the Founder directory');
select throws_ok($$select public.founder_list_audit(null, 1)$$, '42501', null, 'members cannot query Founder audit history');
reset role;

select * from finish();
rollback;
