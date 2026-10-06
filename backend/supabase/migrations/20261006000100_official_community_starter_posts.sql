do $$
declare
  founder_id uuid;
begin
  select pr.user_id into founder_id
  from public.platform_roles pr
  where pr.role = 'founder'
  order by pr.created_at, pr.user_id
  limit 1;

  if founder_id is not null then
    insert into public.posts (id, author_id, topic_id, body, visibility)
    select seed.id, founder_id, topic.id, seed.body, 'public'
    from (values
      (
        'a2026100-0000-4000-8000-000000000002'::uuid,
        'start-here',
        E'Welcome to the TNHC community. Introduce yourself with what you are building or learning, and the kind of people or feedback you hope to find. Browse the shared project catalogue to see what the company is working on and where a conversation might fit.\n\nExplore TNHC: https://tnhc.dev/'
      ),
      (
        'a2026100-0000-4000-8000-000000000003'::uuid,
        'nexus-and-self-hosting',
        E'Welcome to Nexus & Self-Hosting. Nexus is TNHC\'s ecosystem of privacy-first, self-hostable software. Start with a project in the catalogue, then use its repository for current setup instructions and status. When asking for help, include the project, version, install method, and relevant logs with passwords, tokens, and private keys removed.\n\nNexus Systems source: https://github.com/The-No-Hands-company/Nexus-Systems\nCompany project overview: https://github.com/The-No-Hands-company'
      ),
      (
        'a2026100-0000-4000-8000-000000000004'::uuid,
        'developer-workshop',
        E'Bring a focused engineering question: what you expected, what happened, a small reproducible example, and the tools and versions involved. The Nexus Hosting repository has contributor and self-hosting guidance, and welcomes testers and node operators. Check the current README before following setup steps, and remove credentials from logs before sharing them.\n\nNexus Hosting source and guides: https://github.com/The-No-Hands-company/Nexus-Hosting'
      ),
      (
        'a2026100-0000-4000-8000-000000000005'::uuid,
        'creative-projects',
        E'Share the visual, 3D, video, language, and creative-tool work you are exploring. Tell us what you are making, which tools you used, what stage it is in, and the feedback you would find useful. One example from TNHC\'s project catalogue is Nexus Modeling, a geometry and rendering application aimed at production-scale digital-content workflows.\n\nNexus Modeling source: https://github.com/The-No-Hands-company/Nexus-Modeling'
      )
    ) as seed(id, topic_slug, body)
    join public.topics topic on topic.slug = seed.topic_slug
    on conflict (id) do nothing;
  end if;
end;
$$;
