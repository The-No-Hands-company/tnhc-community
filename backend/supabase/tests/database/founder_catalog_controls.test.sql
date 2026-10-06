begin;
select no_plan();
select has_function('public','founder_upsert_project',array['uuid','text','text','text','text','text','text[]','text','text','text'],'Founder project creation and maintenance is available');
select has_function('public','founder_list_topics',array['text','integer'],'Founder topic directory is paginated');
select has_function('public','founder_set_project_membership',array['uuid','uuid','text'],'Founder project membership control is available');
select has_function('public','founder_upsert_topic',array['uuid','text','text','text','text'],'Founder topic creation and maintenance is available');
select has_function('public','founder_set_topic_membership',array['uuid','uuid','boolean'],'Founder topic membership control is available');

insert into auth.users(id,aud,role,email,encrypted_password,email_confirmed_at) values
 ('80000000-0000-0000-0000-000000000001','authenticated','authenticated','catalog-founder@example.test','',now()),
 ('80000000-0000-0000-0000-000000000002','authenticated','authenticated','catalog-member@example.test','',now());
update public.profiles set account_state='active' where id in
 ('80000000-0000-0000-0000-000000000001','80000000-0000-0000-0000-000000000002');
delete from public.platform_roles where role = 'founder';
insert into public.platform_roles(user_id,role) values('80000000-0000-0000-0000-000000000001','founder');
insert into public.projects(id,slug,owner_category,title,summary,stage)
values('80000000-0000-0000-0000-000000000010','catalog-seed','community','Seed','fixture','idea');
insert into public.topics(id,slug,title,visibility,created_at) values
 ('80000000-0000-0000-0000-000000000011','catalog-topic-one','Topic One','private','2099-01-01T00:00:00Z'),
 ('80000000-0000-0000-0000-000000000012','catalog-topic-two','Topic Two','private','2099-01-01T00:00:00Z');

set local role authenticated;
select set_config('request.jwt.claim.sub','80000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claim.role','authenticated',true);
select throws_ok($$select public.founder_upsert_topic(null,'unauthorized-topic','Nope','','public')$$,'42501',null,'regular member cannot create topics');
select throws_ok($$select public.founder_upsert_project(null,'unauthorized-project','community','Nope','','idea','{}','public',null,null)$$,'42501',null,'regular member cannot create projects');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','80000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claim.role','authenticated',true);
select lives_ok($$select public.founder_upsert_project(null,'founder-game','official','Founder Game','A managed project','prototype',array['game','founder'],'members',null,'https://github.com/example/founder-game')$$,'Founder can create a project with source URL');
select is((public.founder_list_topics(null,1)->'items'->0->>'id'),'80000000-0000-0000-0000-000000000012','topic directory applies a stable bounded first page');
select is((public.founder_list_topics(public.founder_list_topics(null,1)->>'next_cursor',1)->'items'->0->>'id'),'80000000-0000-0000-0000-000000000011','topic cursor returns the next tied item');
select is((select count(*)::integer from public.projects where slug='founder-game' and visibility='members'),1,'project fields were stored');
select lives_ok($$select public.founder_upsert_project((select id from public.projects where slug='founder-game'),'founder-game','official','Founder Game Updated','Updated','alpha',array['game'],'public','https://example.com/founder-game','https://github.com/example/founder-game')$$,'Founder can update project links');
select throws_ok($$select public.founder_upsert_project(null,'bad-project-url','official','Bad URL','Invalid source','released',array['source'],'public','http://example.com',null)$$,'22023',null,'Founder cannot save a non-HTTPS project link');
select lives_ok($$select public.founder_set_project_membership((select id from public.projects where slug='founder-game'),'80000000-0000-0000-0000-000000000002','maintainer')$$,'Founder can set a project maintainer');
select is((select role from public.project_memberships where user_id='80000000-0000-0000-0000-000000000002' and project_id=(select id from public.projects where slug='founder-game')),'maintainer','project membership role is stored');
select lives_ok($$select public.founder_upsert_topic(null,'founder-topic','Founder Topic','A managed topic','private')$$,'Founder can create a topic');
select is((select count(*)::integer from public.topics where slug='founder-topic' and visibility='private'),1,'private topic is stored');
select lives_ok($$select public.founder_set_topic_membership((select id from public.topics where slug='founder-topic'),'80000000-0000-0000-0000-000000000002',true)$$,'Founder can add a topic member');
select is((select count(*)::integer from public.topic_memberships where topic_id=(select id from public.topics where slug='founder-topic') and user_id='80000000-0000-0000-0000-000000000002'),1,'topic membership is stored');
select throws_ok($$select public.founder_upsert_topic(null,'Bad Slug','Invalid','Fields','public')$$,'22023',null,'invalid topic slug is rejected');
select is((select count(*)::integer from public.admin_audit_log where actor_id='80000000-0000-0000-0000-000000000001'),5,'catalog and project/topic membership changes are audited');
reset role;
select * from finish();
rollback;
