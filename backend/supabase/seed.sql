-- Synthetic development fixtures only. Do not add member data or import the
-- unverified TNHC project inventory here.

insert into public.projects (slug, owner_category, title, summary, stage, tags, visibility)
values
  ('synthetic-demo-game', 'official', '[DEMO] Untitled Game Prototype', 'Synthetic fixture for local catalogue and policy testing. This is not a verified TNHC project.', 'prototype', array['demo', 'game'], 'public'),
  ('synthetic-demo-tool', 'official', '[DEMO] Community Toolkit', 'Synthetic fixture for local catalogue and policy testing. This is not a verified TNHC project.', 'idea', array['demo', 'tool'], 'public')
on conflict (slug) do update set
  title = excluded.title,
  summary = excluded.summary,
  stage = excluded.stage,
  tags = excluded.tags,
  visibility = excluded.visibility,
  updated_at = now();

-- This local-only account cannot sign in. It exists solely to attribute clearly
-- synthetic development content; it is never created by a public API.
insert into auth.users (id, aud, role, email, encrypted_password, email_confirmed_at)
values ('40000000-0000-0000-0000-000000000001', 'authenticated', 'authenticated', 'synthetic-content@example.test', '', now())
on conflict (id) do nothing;
update public.profiles set account_state = 'pending', display_name = '[DEMO] Local fixture'
where id = '40000000-0000-0000-0000-000000000001';

insert into public.project_memberships (project_id, user_id, role)
select id, '40000000-0000-0000-0000-000000000001', 'maintainer'
from public.projects where slug = 'synthetic-demo-game'
on conflict (project_id, user_id) do update set role = excluded.role;

insert into public.topics (slug, title, description, visibility)
values ('synthetic-demo-topic', '[DEMO] Local topic', 'Synthetic topic fixture for local feed development.', 'public')
on conflict (slug) do update set title = excluded.title, description = excluded.description, visibility = excluded.visibility;
insert into public.topic_memberships (topic_id, user_id)
select id, '40000000-0000-0000-0000-000000000001' from public.topics where slug = 'synthetic-demo-topic'
on conflict (topic_id, user_id) do nothing;

insert into public.posts (id, author_id, project_id, body, visibility)
select '40000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000001', id,
  '[DEMO] Synthetic project update for local feed testing.', visibility
from public.projects where slug = 'synthetic-demo-game'
on conflict (id) do update set body = excluded.body, visibility = excluded.visibility;
insert into public.posts (id, author_id, topic_id, body, visibility)
select '40000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000001', id,
  '[DEMO] Synthetic topic discussion for local feed testing.', visibility
from public.topics where slug = 'synthetic-demo-topic'
on conflict (id) do update set body = excluded.body, visibility = excluded.visibility;
