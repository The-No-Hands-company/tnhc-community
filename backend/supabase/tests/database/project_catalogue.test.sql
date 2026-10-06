begin;
select plan(18);

select has_column('public', 'projects', 'website_url', 'projects retain a public product URL');
select has_column('public', 'projects', 'repository_url', 'projects retain source repository URLs');
select has_column('public', 'projects', 'status_as_of', 'projects expose the date their status was checked');
select ok(exists (
  select 1 from pg_constraint
  where conrelid = 'public.projects'::regclass
    and contype = 'c'
    and pg_get_constraintdef(oid) ilike '%in_development%'
), 'in_development is an allowed project stage');
select is((select count(*)::integer from public.projects where owner_category = 'official' and status_as_of = date '2026-08-19'), 32, 'catalogue includes the 32 approved code-bearing Nexus projects');
select is((select count(*)::integer from public.projects where owner_category = 'community' and slug = 'devtrack'), 1, 'DevTrack is represented as a member-owned project');
select is((select count(*)::integer from public.projects where stage = 'released' and owner_category = 'official' and status_as_of = date '2026-08-19'), 6, 'six Nexus projects are marked released by the dated register');
select is((select count(*)::integer from public.projects where stage = 'beta' and owner_category = 'official' and status_as_of = date '2026-08-19'), 1, 'Nexus Email is marked beta by the dated register');
select is((select count(*)::integer from public.projects where stage = 'in_development' and owner_category = 'official' and status_as_of = date '2026-08-19'), 25, '25 Nexus projects retain the dated in-development status');
select is((select count(*)::integer from public.projects where status_as_of = date '2026-08-19' and owner_category = 'official'), 32, 'all Nexus statuses disclose the register date');
select is((select count(*)::integer from public.topics where visibility = 'public' and slug not like 'synthetic-%'), 5, 'five public topic communities are available');
select ok(not exists (select 1 from public.platform_roles where role = 'founder') or
  (select count(*)::integer from public.posts p join public.topics t on t.id = p.topic_id
    where t.slug = 'project-releases' and p.body like '%https://zajfan.tnhc.dev/devlog/devtrack-1-0-released/%') = 1,
  'a configured Founder account receives the sourced DevTrack release post');
select is((select count(*)::integer from public.projects where website_url is not null and website_url !~ '^https://'), 0, 'catalogue website links use HTTPS');
select is((select count(*)::integer from public.projects where repository_url is not null and repository_url !~ '^https://'), 0, 'catalogue source links use HTTPS');
select is((select count(*)::integer from jsonb_array_elements(public.list_projects(null, 7, null, null, null)->'items') item where item->>'stage' = 'released'), 7, 'all released projects sort ahead of beta and development');
select is((public.list_projects(null, 8, null, null, null)->'items'->7->>'stage'), 'beta', 'beta projects follow released projects in catalogue order');
select throws_ok($$insert into public.projects(slug,owner_category,title,summary,stage,website_url) values ('invalid-catalogue-link','community','Invalid Link','test','idea','http://example.com')$$, '23514', null, 'project website rejects non-HTTPS URLs');
select throws_ok($$insert into public.projects(slug,owner_category,title,summary,stage,repository_url) values ('invalid-catalogue-source','community','Invalid Source','test','idea','javascript:alert(1)')$$, '23514', null, 'project source rejects unsafe URL schemes');

select * from finish();
rollback;
