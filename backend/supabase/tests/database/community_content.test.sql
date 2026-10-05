begin;
select no_plan();

select has_table('public', 'topics', 'topics are installed');
select has_table('public', 'topic_memberships', 'topic memberships are installed');
select has_table('public', 'posts', 'posts are installed');
select has_table('public', 'comments', 'comments are installed');

select ok((select relrowsecurity from pg_class where oid='public.topics'::regclass), 'topics enforce row-level security');
select ok((select relrowsecurity from pg_class where oid='public.topic_memberships'::regclass), 'topic memberships enforce row-level security');
select ok((select relrowsecurity from pg_class where oid='public.posts'::regclass), 'posts enforce row-level security');
select ok((select relrowsecurity from pg_class where oid='public.comments'::regclass), 'comments enforce row-level security');

select has_column('public', 'topics', 'visibility', 'topics declare a visibility scope');
select has_column('public', 'posts', 'author_id', 'posts store a server-attributed author');
select has_column('public', 'posts', 'project_id', 'posts can belong to a project');
select has_column('public', 'posts', 'topic_id', 'posts can belong to a topic');
select has_column('public', 'posts', 'moderation_state', 'posts have a moderation state');
select has_column('public', 'comments', 'author_id', 'comments store a server-attributed author');
select has_column('public', 'comments', 'moderation_state', 'comments have a moderation state');

select ok(exists(select 1 from pg_constraint where conrelid='public.posts'::regclass and contype='c' and pg_get_constraintdef(oid) ilike '%project_id%topic_id%'), 'posts require exactly one content context');
select ok(exists(select 1 from pg_constraint where conrelid='public.posts'::regclass and contype='c' and pg_get_constraintdef(oid) ilike '%length%body%'), 'post body length is bounded');
select ok(exists(select 1 from pg_constraint where conrelid='public.comments'::regclass and contype='c' and pg_get_constraintdef(oid) ilike '%length%body%'), 'comment body length is bounded');
select ok(exists(select 1 from pg_constraint where conrelid='public.posts'::regclass and contype='f' and confrelid='auth.users'::regclass), 'posts reference Auth users');
select ok(exists(select 1 from pg_constraint where conrelid='public.posts'::regclass and contype='f' and confrelid='public.projects'::regclass), 'project posts reference projects');
select ok(exists(select 1 from pg_constraint where conrelid='public.posts'::regclass and contype='f' and confrelid='public.topics'::regclass), 'topic posts reference topics');
select ok(exists(select 1 from pg_constraint where conrelid='public.comments'::regclass and contype='f' and confrelid='public.posts'::regclass), 'comments reference posts');
select ok(exists(select 1 from pg_indexes where schemaname='public' and indexname='posts_project_feed_order'), 'project feed index is present');
select ok(exists(select 1 from pg_indexes where schemaname='public' and indexname='posts_topic_feed_order'), 'topic feed index is present');
select ok(exists(select 1 from pg_indexes where schemaname='public' and indexname='comments_post_order'), 'comment ordering index is present');

insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at)
values
 ('30000000-0000-0000-0000-000000000001', 'authenticated', 'authenticated', 'content-author@example.test', '', now()),
 ('30000000-0000-0000-0000-000000000002', 'authenticated', 'authenticated', 'content-other@example.test', '', now()),
 ('30000000-0000-0000-0000-000000000003', 'authenticated', 'authenticated', 'content-maintainer@example.test', '', now());
update public.profiles set account_state='active' where id in (
 '30000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000002',
 '30000000-0000-0000-0000-000000000003');
insert into public.projects (id, slug, owner_category, title, summary, stage, visibility)
values ('30000000-0000-0000-0000-000000000010', 'content-demo', 'official', '[DEMO] Content test', 'Synthetic fixture', 'prototype', 'public');
insert into public.project_memberships (project_id, user_id, role)
values ('30000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000003', 'maintainer');
insert into public.topics (id, slug, title, description, visibility)
values ('30000000-0000-0000-0000-000000000020', 'content-demo', '[DEMO] Topic', 'Synthetic fixture', 'public');

select throws_ok($$insert into public.posts(author_id, body) values ('30000000-0000-0000-0000-000000000001', 'missing context')$$, '23514', null, 'post without project or topic context is rejected');
select throws_ok($$insert into public.posts(author_id, project_id, topic_id, body) values ('30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000020', 'ambiguous context')$$, '23514', null, 'post with two contexts is rejected');
select throws_ok($$insert into public.posts(author_id, topic_id, body) values ('30000000-0000-0000-0000-000000000099', '30000000-0000-0000-0000-000000000020', 'bad author')$$, '23503', null, 'post requires an existing Auth author');
select throws_ok($$insert into public.comments(post_id, author_id, body) values ('30000000-0000-0000-0000-000000000099', '30000000-0000-0000-0000-000000000001', 'orphan')$$, '23503', null, 'comment requires an existing post');
select throws_ok(format('insert into public.posts(author_id, topic_id, body) values (%L, %L, %L)', '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000020', repeat('x', 5001)), '23514', null, 'oversize post is rejected');
insert into public.posts (id, author_id, project_id, body, visibility, moderation_state)
values ('30000000-0000-0000-0000-000000000030', '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000010', 'member-authored update', 'public', 'visible');
insert into public.comments (id, post_id, author_id, body, moderation_state)
values ('30000000-0000-0000-0000-000000000040', '30000000-0000-0000-0000-000000000030', '30000000-0000-0000-0000-000000000001', 'author comment', 'visible');
select throws_ok(format('insert into public.comments(post_id, author_id, body) values (%L, %L, %L)', '30000000-0000-0000-0000-000000000030', '30000000-0000-0000-0000-000000000001', repeat('x', 2001)), '23514', null, 'oversize comment is rejected');
select is((select author_id from public.posts where id='30000000-0000-0000-0000-000000000030'), '30000000-0000-0000-0000-000000000001'::uuid, 'post author is stored');
select is((select author_id from public.comments where id='30000000-0000-0000-0000-000000000040'), '30000000-0000-0000-0000-000000000001'::uuid, 'comment author is stored');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='posts' and policyname='posts_update_author'), 'post updates have an author-only policy');
select ok(exists(select 1 from pg_policies where schemaname='public' and tablename='comments' and policyname='comments_update_author'), 'comment updates have an author-only policy');

set local role authenticated;
select set_config('request.jwt.claim.sub', '30000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is((select count(*)::integer from public.posts where id='30000000-0000-0000-0000-000000000030'), 1, 'active members can read visible public posts');
insert into public.topic_memberships(topic_id) values ('30000000-0000-0000-0000-000000000020');
insert into public.posts(topic_id, body) values ('30000000-0000-0000-0000-000000000020', 'topic post');
select is((select author_id from public.posts where topic_id='30000000-0000-0000-0000-000000000020'), '30000000-0000-0000-0000-000000000001'::uuid, 'post author is derived from the authenticated session');
select throws_ok($$insert into public.posts(project_id, body) values ('30000000-0000-0000-0000-000000000010', 'unauthorized official update')$$, '42501', null, 'official project updates require maintainer membership');
select lives_ok($$update public.posts set body='author edited update' where id='30000000-0000-0000-0000-000000000030'$$, 'author can edit own post');
select lives_ok($$update public.comments set body='author edited comment' where id='30000000-0000-0000-0000-000000000040'$$, 'author can edit own comment');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '30000000-0000-0000-0000-000000000002', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select is((select count(*)::integer from public.posts where author_id='30000000-0000-0000-0000-000000000001'), 2, 'another active member can read visible public project and topic posts');
select lives_ok($test$do $body$ declare changed integer; begin
 update public.posts set body='unauthorized edit' where id='30000000-0000-0000-0000-000000000030';
 get diagnostics changed = row_count;
 if changed <> 0 then raise exception 'cross-author update affected rows'; end if;
end $body$$test$, 'another member cannot edit a post');
with removed as (delete from public.posts where id='30000000-0000-0000-0000-000000000030' returning *)
select is((select count(*)::integer from removed), 0, 'another member cannot delete a post');
select is((select count(*)::integer from public.posts where id='30000000-0000-0000-0000-000000000030'), 1, 'cross-author delete leaves the post intact');
select lives_ok($test$do $body$ declare changed integer; begin
 update public.comments set body='unauthorized comment edit' where id='30000000-0000-0000-0000-000000000040';
 get diagnostics changed = row_count;
 if changed <> 0 then raise exception 'cross-author comment update affected rows'; end if;
end $body$$test$, 'another member cannot edit a comment');
reset role;

set local role authenticated;
select set_config('request.jwt.claim.sub', '30000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);
select lives_ok($$delete from public.comments where id='30000000-0000-0000-0000-000000000040'$$, 'author can delete own comment');
reset role;

select * from finish();
rollback;
