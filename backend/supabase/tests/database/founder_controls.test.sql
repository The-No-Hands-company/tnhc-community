begin;
select no_plan();

select ok(to_regclass('public.admin_audit_log') is not null, 'append-only admin audit table exists');
select ok(to_regclass('public.app_settings') is not null, 'application settings table exists');
select ok(to_regprocedure('public.founder_set_member_state(uuid,text)') is not null, 'member state control exists');
select ok(to_regprocedure('public.founder_set_platform_role(uuid,text,boolean)') is not null, 'platform role control exists');
select ok(to_regprocedure('public.founder_set_app_setting(text,jsonb)') is not null, 'application setting control exists');

insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at)
values
 ('60000000-0000-0000-0000-000000000001', 'authenticated', 'authenticated', 'controls-founder@example.test', '', now()),
 ('60000000-0000-0000-0000-000000000002', 'authenticated', 'authenticated', 'controls-member@example.test', '', now()),
 ('60000000-0000-0000-0000-000000000003', 'authenticated', 'authenticated', 'controls-moderator@example.test', '', now());
update public.profiles set account_state='active' where id in (
 '60000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002',
 '60000000-0000-0000-0000-000000000003');
insert into public.platform_roles (user_id, role)
values ('60000000-0000-0000-0000-000000000001', 'founder');

set local role authenticated;
select set_config('request.jwt.claim.sub', '60000000-0000-0000-0000-000000000002', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select throws_ok($$select public.founder_set_member_state('60000000-0000-0000-0000-000000000002', 'suspended')$$, '42501', null, 'ordinary member cannot suspend an account');
select throws_ok($$select public.founder_set_app_setting('maintenance_notice', '"offline"'::jsonb)$$, '42501', null, 'ordinary member cannot change application settings');
select is((select count(*)::integer from public.admin_audit_log), 0, 'ordinary member cannot inspect Founder audit history');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '60000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select lives_ok($$select public.founder_set_member_state('60000000-0000-0000-0000-000000000002', 'suspended')$$, 'Founder can suspend a member');
select is((select account_state from public.profiles where id='60000000-0000-0000-0000-000000000002'), 'suspended', 'member suspension is applied');
select is((select count(*)::integer from public.admin_audit_log where actor_id='60000000-0000-0000-0000-000000000001' and action='member_state_changed'), 1, 'member suspension is audited once');
select throws_ok($$select public.founder_set_member_state('60000000-0000-0000-0000-000000000002', 'pending')$$, '22023', null, 'Founder cannot set a member to a system-only pending state');
select is((select count(*)::integer from public.admin_audit_log where actor_id='60000000-0000-0000-0000-000000000001'), 1, 'failed state changes do not create audit records');
select lives_ok($$select public.founder_set_platform_role('60000000-0000-0000-0000-000000000003', 'moderator', true)$$, 'Founder can grant a moderator role');
select is((select count(*)::integer from public.platform_roles where user_id='60000000-0000-0000-0000-000000000003' and role='moderator'), 1, 'moderator role is applied');
select throws_ok($$select public.founder_set_platform_role('60000000-0000-0000-0000-000000000003', 'founder', true)$$, '22023', null, 'Founder cannot assign another Founder');
select lives_ok($$select public.founder_set_app_setting('maintenance_notice', '"Service maintenance at 18:00"'::jsonb)$$, 'Founder can set a maintenance notice');
select is((select value from public.app_settings where key='maintenance_notice'), '"Service maintenance at 18:00"'::jsonb, 'maintenance notice is readable to clients');
select lives_ok($$select public.founder_set_app_setting('feature_flags', '{"community_feed":true,"messaging":false}'::jsonb)$$, 'Founder can set typed feature availability flags');
select throws_ok($$select public.founder_set_app_setting('arbitrary_sql', '"select 1"'::jsonb)$$, '22023', null, 'Founder cannot set unlisted application configuration');
select throws_ok($$select public.founder_set_app_setting('maintenance_notice', '123'::jsonb)$$, '22023', null, 'maintenance notice must be a string');
select is((select count(*)::integer from public.admin_audit_log where actor_id='60000000-0000-0000-0000-000000000001'), 4, 'every successful Founder mutation is audited and failures are omitted');
select throws_ok($$update public.admin_audit_log set summary='rewritten' where actor_id='60000000-0000-0000-0000-000000000001'$$, '42501', null, 'Founder cannot rewrite audit history');
select throws_ok($$delete from public.admin_audit_log where actor_id='60000000-0000-0000-0000-000000000001'$$, '42501', null, 'Founder cannot delete audit history');
select throws_ok($$update public.platform_roles set role='founder' where user_id='60000000-0000-0000-0000-000000000002'$$, '42501', null, 'Founder cannot change role assignments outside the controlled function');
reset role;

set local role anon;
select is((select value from public.app_settings where key='maintenance_notice'), '"Service maintenance at 18:00"'::jsonb, 'anonymous app clients can read the public maintenance notice');
select throws_ok($$select public.founder_set_app_setting('maintenance_notice', '"hacked"'::jsonb)$$, '42501', null, 'anonymous caller cannot change app settings');
reset role;

select * from finish();
rollback;
