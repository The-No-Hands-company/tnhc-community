begin;
select no_plan();
select has_function('public','founder_list_content',array['text','text','integer'],'Founder content review is bounded and paginated');
select has_function('public','founder_set_content_moderation',array['text','uuid','text'],'Founder content moderation control exists');
insert into auth.users(id,aud,role,email,encrypted_password,email_confirmed_at) values
 ('90000000-0000-0000-0000-000000000001','authenticated','authenticated','content-founder@example.test','',now()),
 ('90000000-0000-0000-0000-000000000002','authenticated','authenticated','content-member@example.test','',now());
update public.profiles set account_state='active' where id in ('90000000-0000-0000-0000-000000000001','90000000-0000-0000-0000-000000000002');
delete from public.platform_roles where role = 'founder';
insert into public.platform_roles(user_id,role) values('90000000-0000-0000-0000-000000000001','founder');
insert into public.topics(id,slug,title,visibility) values('90000000-0000-0000-0000-000000000010','review-topic','Review Topic','public');
insert into public.posts(id,author_id,topic_id,body) values('90000000-0000-0000-0000-000000000020','90000000-0000-0000-0000-000000000002','90000000-0000-0000-0000-000000000010','review post body');
insert into public.comments(id,post_id,author_id,body) values('90000000-0000-0000-0000-000000000030','90000000-0000-0000-0000-000000000020','90000000-0000-0000-0000-000000000002','review comment body');

set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000002',true);
select set_config('request.jwt.claim.role','authenticated',true);
select throws_ok($$select public.founder_list_content('hidden',null,20)$$,'42501',null,'ordinary members cannot inspect Founder moderation queue');
select throws_ok($$select public.founder_set_content_moderation('post','90000000-0000-0000-0000-000000000020','hidden')$$,'42501',null,'ordinary members cannot moderate content');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub','90000000-0000-0000-0000-000000000001',true);
select set_config('request.jwt.claim.role','authenticated',true);
select lives_ok($$select public.founder_set_content_moderation('post','90000000-0000-0000-0000-000000000020','hidden')$$,'Founder can hide a post');
select lives_ok($$select public.founder_set_content_moderation('comment','90000000-0000-0000-0000-000000000030','removed')$$,'Founder can remove a comment');
select ok((public.founder_list_content('hidden',null,20)->'items') @> '[{"content_type":"post","id":"90000000-0000-0000-0000-000000000020","body":"review post body"}]'::jsonb,'Founder queue includes bounded post details for review');
select ok((public.founder_list_content('removed',null,20)->'items') @> '[{"content_type":"comment","id":"90000000-0000-0000-0000-000000000030","body":"review comment body","moderation_state":"removed"}]'::jsonb,'Founder queue reflects removed comment state');
select ok(not exists(select 1 from public.admin_audit_log where actor_id='90000000-0000-0000-0000-000000000001' and summary ilike '%review post body%'),'audit record does not copy content body');
select is((select count(*)::integer from public.admin_audit_log where actor_id='90000000-0000-0000-0000-000000000001' and action='content_moderation_changed'),2,'each moderation change is audited');
select throws_ok($$select public.founder_set_content_moderation('message','90000000-0000-0000-0000-000000000020','hidden')$$,'22023',null,'message content is outside this moderation interface');
select throws_ok($$select public.founder_set_content_moderation('post','90000000-0000-0000-0000-000000000020','invalid')$$,'22023',null,'invalid moderation state is rejected');
reset role;
select * from finish();
rollback;
