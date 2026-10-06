insert into public.projects
  (slug, owner_category, title, summary, stage, tags, visibility, website_url, repository_url, status_as_of)
values
  ('nexus', 'official', 'Nexus', 'Real-time chat runtime for the Nexus ecosystem.', 'released', array['Core','communication','nexus'], 'public', 'https://chat.tnhc.dev/', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus', date '2026-08-19'),
  ('nexus-dashboard', 'official', 'Nexus Dashboard', 'Ecosystem shell for app access, accounts, administration, and service views.', 'released', array['Core','dashboard','nexus'], 'public', 'https://app.tnhc.dev/', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Dashboard', date '2026-08-19'),
  ('nexus-auth', 'official', 'Nexus Auth', 'OpenID Connect identity provider for Nexus applications.', 'released', array['Core','identity','nexus'], 'public', 'https://auth.tnhc.dev/', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Auth', date '2026-08-19'),
  ('nexus-cloud', 'official', 'Nexus Cloud', 'Control plane for service registration, federation, and operator workflows.', 'released', array['Core','cloud','nexus'], 'public', 'https://cloud.tnhc.dev/', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Cloud', date '2026-08-19'),
  ('nexus-hosting', 'official', 'Nexus Hosting', 'Self-hostable website hosting with domains, TLS, builds, and forms.', 'released', array['Infrastructure','hosting','nexus'], 'public', 'https://hosting.tnhc.dev/', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Hosting', date '2026-08-19'),
  ('nexus-draw', 'official', 'Nexus Draw', 'Whiteboard and diagramming tool for visual collaboration.', 'released', array['Creative','design','nexus'], 'public', 'https://draw.tnhc.dev/', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Draw', date '2026-08-19'),
  ('nexus-email', 'official', 'Nexus Email', 'Self-hosted email services with SMTP, IMAP, authentication records, and webmail.', 'beta', array['Communication','email','nexus'], 'public', 'https://app.tnhc.dev/mail', 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Email', date '2026-08-19'),
  ('nexus-deploy', 'official', 'Nexus Deploy', 'Deployment orchestration and release pipelines.', 'in_development', array['Infrastructure','deployment','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Deploy', date '2026-08-19'),
  ('nexus-tunnel', 'official', 'Nexus Tunnel', 'Public service exposure and tunnel management for Nexus deployments.', 'in_development', array['Infrastructure','networking','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Tunnel', date '2026-08-19'),
  ('nexus-router', 'official', 'Nexus Router', 'Request routing and upstream selection across services.', 'in_development', array['Infrastructure','networking','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Router', date '2026-08-19'),
  ('nexus-edge', 'official', 'Nexus Edge', 'Edge termination and regional request handling.', 'in_development', array['Infrastructure','networking','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Edge', date '2026-08-19'),
  ('nexus-network', 'official', 'Nexus Network', 'Network topology and connectivity management for the ecosystem.', 'in_development', array['Infrastructure','networking','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Network', date '2026-08-19'),
  ('nexus-monitor', 'official', 'Nexus Monitor', 'Health checks, uptime information, and operational alerts.', 'in_development', array['Infrastructure','operations','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Monitor', date '2026-08-19'),
  ('nexus-engine', 'official', 'Nexus Engine', 'Execution engine for workloads in the Nexus ecosystem.', 'in_development', array['Compute','runtime','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Engine', date '2026-08-19'),
  ('nexus-computer', 'official', 'Nexus Computer', 'Virtual machine and workstation provisioning for personal computing.', 'in_development', array['Compute','workstation','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Computer', date '2026-08-19'),
  ('nexus-gpu-test', 'official', 'Nexus GPU Test', 'GPU capability probing and benchmark tooling.', 'in_development', array['Compute','gpu','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-GPU-Test', date '2026-08-19'),
  ('nexus-forge', 'official', 'Nexus Forge', 'Build and artifact forge for software projects.', 'in_development', array['Compute','build','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Forge', date '2026-08-19'),
  ('nexus-database', 'official', 'Nexus Database', 'NexusDB: a consistency-tiered database engine with C++ core and Rust federation.', 'in_development', array['Data','database','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Database', date '2026-08-19'),
  ('nexus-vault', 'official', 'Nexus Vault', 'Secrets and credential storage for Nexus services.', 'in_development', array['Security','secrets','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Vault', date '2026-08-19'),
  ('nexus-guardian', 'official', 'Nexus Guardian', 'Runtime threat monitoring and abuse response for connected services.', 'in_development', array['Security','monitoring','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Guardian', date '2026-08-19'),
  ('phantom', 'official', 'Phantom', 'Protocol security layer exploring privacy-preserving routing and membership proofs.', 'in_development', array['Security','protocol','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Phantom', date '2026-08-19'),
  ('nexus-ai', 'official', 'Nexus AI', 'Core AI services and model access for Nexus applications.', 'in_development', array['AI','models','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-AI', date '2026-08-19'),
  ('nexus-ai-hub', 'official', 'Nexus AI Hub', 'Catalogue and routing across hosted providers and local AI models.', 'in_development', array['AI','models','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-AI-Hub', date '2026-08-19'),
  ('nexus-agents', 'official', 'Nexus Agents', 'Agent runtime and tool-use services for the Nexus ecosystem.', 'in_development', array['AI','agents','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Agents', date '2026-08-19'),
  ('nexus-code', 'official', 'Nexus Code', 'Code hosting and repository management for developers.', 'in_development', array['Developer Tools','code','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Code', date '2026-08-19'),
  ('nexus-ide', 'official', 'Nexus IDE', 'Integrated development environment for building software.', 'in_development', array['Developer Tools','ide','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-IDE', date '2026-08-19'),
  ('nexus-wiki', 'official', 'Nexus Wiki', 'Structured wiki and knowledge pages for teams and projects.', 'in_development', array['Productivity','knowledge','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Wiki', date '2026-08-19'),
  ('nexus-design', 'official', 'Nexus Design', 'Interface and graphic design tooling for the Nexus suite.', 'in_development', array['Creative','design','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Design', date '2026-08-19'),
  ('nexus-graphic', 'official', 'Nexus Graphic', 'Raster and vector graphics tools.', 'in_development', array['Creative','graphics','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Graphic', date '2026-08-19'),
  ('nexus-modeling', 'official', 'Nexus Modeling', '3D modelling kernel and workflows for digital content creation.', 'in_development', array['Creative','3d','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Modeling', date '2026-08-19'),
  ('nexus-photos', 'official', 'Nexus Photos', 'Photo library and editing tools.', 'in_development', array['Creative','photography','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Photos', date '2026-08-19'),
  ('nexus-video', 'official', 'Nexus Video', 'Video editing and processing tools.', 'in_development', array['Creative','video','nexus'], 'public', null, 'https://github.com/The-No-Hands-company/Nexus-Systems/tree/main/apps/Nexus-Video', date '2026-08-19'),
  ('devtrack', 'community', 'DevTrack', 'Local-first project management for solo developers. DevTrack 1.0 was released on 2026-10-02 for desktop and Android, with projects, tasks, notes, time tracking, Git awareness, and local SQLite storage.', 'released', array['Developer Tools','project-management','community'], 'public', 'https://zajfan.tnhc.dev/devlog/devtrack-1-0-released/', 'https://github.com/Zajfan/DevTrack', date '2026-10-02')
on conflict (slug) do update set
  owner_category = excluded.owner_category,
  title = excluded.title,
  summary = excluded.summary,
  stage = excluded.stage,
  tags = excluded.tags,
  visibility = excluded.visibility,
  website_url = excluded.website_url,
  repository_url = excluded.repository_url,
  status_as_of = excluded.status_as_of,
  updated_at = now();

insert into public.topics (slug, title, description, visibility)
values
  ('start-here', 'Start Here', 'Introduce yourself, ask how TNHC works, and find the right place for a conversation.', 'public'),
  ('nexus-and-self-hosting', 'Nexus & Self-Hosting', 'Discuss the Nexus ecosystem, running services yourself, and operating them responsibly.', 'public'),
  ('developer-workshop', 'Developer Workshop', 'Share questions and practical work around code, tools, languages, and building software.', 'public'),
  ('creative-projects', 'Creative Projects', 'Talk about design, graphics, 3D, photography, video, and creative tools.', 'public'),
  ('project-releases', 'Project Releases', 'Post real release notes and meaningful project updates with links to their sources.', 'public')
on conflict (slug) do update set
  title = excluded.title,
  description = excluded.description,
  visibility = excluded.visibility;

do $$
declare
  founder_id uuid;
  releases_topic_id uuid;
begin
  select pr.user_id into founder_id
  from public.platform_roles pr
  where pr.role = 'founder'
  order by pr.created_at, pr.user_id
  limit 1;

  if founder_id is not null then
    insert into public.project_memberships (project_id, user_id, role)
    select p.id, founder_id, 'owner'
    from public.projects p
    where p.slug = 'devtrack'
    on conflict (project_id, user_id) do update set role = excluded.role;

    select id into releases_topic_id from public.topics where slug = 'project-releases';
    insert into public.posts (id, author_id, topic_id, body, visibility)
    values (
      'a2026100-0000-4000-8000-000000000001',
      founder_id,
      releases_topic_id,
      E'DevTrack 1.0 is released. The local-first project manager is available for Windows, macOS, Linux, and Android, with project and task planning, notes, time tracking, Git awareness, and offline SQLite storage. Desktop and Android data transfer is currently manual.\n\nRelease article: https://zajfan.tnhc.dev/devlog/devtrack-1-0-released/\nDownloads and source: https://github.com/Zajfan/DevTrack/releases',
      'public'
    )
    on conflict (id) do update set
      author_id = excluded.author_id,
      topic_id = excluded.topic_id,
      body = excluded.body,
      visibility = excluded.visibility,
      moderation_state = 'visible';
  end if;
end;
$$;

insert into public.project_memberships (project_id, user_id, role)
select p.id, pr.user_id, 'owner'
from public.projects p
cross join lateral (
  select user_id from public.platform_roles where role = 'founder' order by created_at, user_id limit 1
) pr
where p.owner_category = 'official'
on conflict (project_id, user_id) do update set role = excluded.role;
