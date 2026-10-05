create table public.topics (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique check (slug = lower(slug) and slug ~ '^[a-z0-9]+([a-z0-9-]*[a-z0-9])?$'),
  title text not null check (length(btrim(title)) between 1 and 160),
  description text not null default '' check (length(description) <= 2000),
  visibility text not null default 'public' check (visibility in ('public', 'members', 'private')),
  created_at timestamptz not null default now()
);

create table public.topic_memberships (
  topic_id uuid not null references public.topics(id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (topic_id, user_id)
);

create table public.posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references auth.users(id) on delete restrict,
  project_id uuid references public.projects(id) on delete cascade,
  topic_id uuid references public.topics(id) on delete cascade,
  body text not null check (length(btrim(body)) between 1 and 5000),
  visibility text not null default 'public' check (visibility in ('public', 'members', 'private')),
  moderation_state text not null default 'visible' check (moderation_state in ('visible', 'hidden', 'removed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((project_id is not null) <> (topic_id is not null))
);

create table public.comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.posts(id) on delete cascade,
  author_id uuid not null references auth.users(id) on delete restrict,
  body text not null check (length(btrim(body)) between 1 and 2000),
  moderation_state text not null default 'visible' check (moderation_state in ('visible', 'hidden', 'removed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index topic_memberships_by_member on public.topic_memberships(user_id, topic_id);
create index posts_project_feed_order on public.posts(project_id, created_at desc, id desc);
create index posts_topic_feed_order on public.posts(topic_id, created_at desc, id desc);
create index posts_author_order on public.posts(author_id, created_at desc, id desc);
create index comments_post_order on public.comments(post_id, created_at desc, id desc);

create trigger posts_set_updated_at before update on public.posts
for each row execute function public.set_updated_at();
create trigger comments_set_updated_at before update on public.comments
for each row execute function public.set_updated_at();

create function public.attribute_post_author()
returns trigger language plpgsql set search_path = '' as $$
begin
  if (select auth.uid()) is not null then
    new.author_id := (select auth.uid());
    new.moderation_state := 'visible';
    if new.project_id is not null then
      select p.visibility into new.visibility from public.projects p where p.id = new.project_id;
    else
      select t.visibility into new.visibility from public.topics t where t.id = new.topic_id;
    end if;
  end if;
  return new;
end;
$$;

create function public.attribute_comment_author()
returns trigger language plpgsql set search_path = '' as $$
begin
  if (select auth.uid()) is not null then
    new.author_id := (select auth.uid());
    new.moderation_state := 'visible';
  end if;
  return new;
end;
$$;

create trigger posts_attribute_author before insert on public.posts
for each row execute function public.attribute_post_author();
create trigger comments_attribute_author before insert on public.comments
for each row execute function public.attribute_comment_author();

create function public.viewer_is_topic_member(p_topic_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.topic_memberships tm
    where tm.topic_id = p_topic_id and tm.user_id = (select auth.uid())
  );
$$;
revoke all on function public.viewer_is_topic_member(uuid) from public, anon;
grant execute on function public.viewer_is_topic_member(uuid) to authenticated;

alter table public.topics enable row level security;
alter table public.topic_memberships enable row level security;
alter table public.posts enable row level security;
alter table public.comments enable row level security;

create policy topics_read_public on public.topics for select to anon, authenticated
using (visibility = 'public');
create policy topics_read_members on public.topics for select to authenticated
using (visibility = 'members' and (select public.viewer_is_active_member()));
create policy topics_read_private_members on public.topics for select to authenticated
using (visibility = 'private' and (select public.viewer_is_topic_member(topics.id)));

create policy topic_memberships_read_self on public.topic_memberships for select to authenticated
using (user_id = (select auth.uid()));
create policy topic_memberships_join_visible on public.topic_memberships for insert to authenticated
with check (
  user_id = (select auth.uid())
  and (select public.viewer_is_active_member())
  and exists (select 1 from public.topics t where t.id = topic_memberships.topic_id and t.visibility in ('public', 'members'))
);
create policy topic_memberships_leave_self on public.topic_memberships for delete to authenticated
using (user_id = (select auth.uid()));

create policy posts_read_visible on public.posts for select to anon, authenticated
using (
  moderation_state = 'visible'
  and (
    (project_id is not null and exists (
      select 1 from public.projects p where p.id = posts.project_id and p.visibility = 'public'
        and posts.visibility = p.visibility
    ))
    or (topic_id is not null and exists (
      select 1 from public.topics t where t.id = posts.topic_id and t.visibility = 'public'
        and posts.visibility = t.visibility
    ))
  )
);
create policy posts_read_members on public.posts for select to authenticated
using (
  moderation_state = 'visible'
  and (select public.viewer_is_active_member())
  and (
    (project_id is not null and exists (
      select 1 from public.projects p where p.id = posts.project_id and p.visibility = 'members'
        and posts.visibility = p.visibility
    ))
    or (topic_id is not null and exists (
      select 1 from public.topics t where t.id = posts.topic_id and t.visibility = 'members'
        and posts.visibility = t.visibility
    ))
  )
);
create policy posts_read_private_authorized on public.posts for select to authenticated
using (
  author_id = (select auth.uid())
  or (
    moderation_state = 'visible'
    and ((project_id is not null and exists (
      select 1 from public.project_memberships pm where pm.project_id = posts.project_id and pm.user_id = (select auth.uid())
    )) or (topic_id is not null and exists (
      select 1 from public.topic_memberships tm where tm.topic_id = posts.topic_id and tm.user_id = (select auth.uid())
    )))
  )
);
create policy posts_insert_authorized on public.posts for insert to authenticated
with check (
  author_id = (select auth.uid())
  and (select public.viewer_is_active_member())
  and (
    (project_id is not null and exists (
      select 1 from public.projects p where p.id = posts.project_id and posts.visibility = p.visibility
        and exists (select 1 from public.project_memberships pm where pm.project_id = p.id and pm.user_id = (select auth.uid()))
        and (p.owner_category <> 'official' or exists (
          select 1 from public.project_memberships pm where pm.project_id = p.id and pm.user_id = (select auth.uid()) and pm.role in ('owner', 'maintainer')
        ))
    ))
    or (topic_id is not null and exists (
      select 1 from public.topics t where t.id = posts.topic_id and posts.visibility = t.visibility
        and exists (select 1 from public.topic_memberships tm where tm.topic_id = t.id and tm.user_id = (select auth.uid()))
    ))
  )
);
create policy posts_update_author on public.posts for update to authenticated
using (author_id = (select auth.uid()))
with check (author_id = (select auth.uid()));
create policy posts_delete_author on public.posts for delete to authenticated
using (author_id = (select auth.uid()));

create policy comments_read_visible on public.comments for select to anon, authenticated
using (moderation_state = 'visible' and exists (
  select 1 from public.posts p where p.id = comments.post_id and p.moderation_state = 'visible'
    and ((p.project_id is not null and p.visibility = 'public') or (p.topic_id is not null and p.visibility = 'public'))
));
create policy comments_read_members on public.comments for select to authenticated
using (moderation_state = 'visible' and exists (
  select 1 from public.posts p where p.id = comments.post_id and p.moderation_state = 'visible'
    and (p.author_id = (select auth.uid()) or exists (
      select 1 from public.projects pr where pr.id = p.project_id and pr.visibility in ('public', 'members')
    ) or exists (
      select 1 from public.topics t where t.id = p.topic_id and t.visibility in ('public', 'members')
    ) or exists (
      select 1 from public.project_memberships pm where pm.project_id = p.project_id and pm.user_id = (select auth.uid())
    ) or exists (
      select 1 from public.topic_memberships tm where tm.topic_id = p.topic_id and tm.user_id = (select auth.uid())
    ))
));
create policy comments_insert_authorized on public.comments for insert to authenticated
with check (
  author_id = (select auth.uid())
  and (select public.viewer_is_active_member())
  and exists (
    select 1 from public.posts p where p.id = comments.post_id and p.moderation_state = 'visible'
      and ((p.visibility in ('public', 'members') and (
        (p.project_id is not null and exists (select 1 from public.projects pr where pr.id = p.project_id and pr.visibility = p.visibility))
        or (p.topic_id is not null and exists (select 1 from public.topics t where t.id = p.topic_id and t.visibility = p.visibility))
      )) or p.author_id = (select auth.uid())
      or exists (select 1 from public.project_memberships pm where pm.project_id = p.project_id and pm.user_id = (select auth.uid()))
      or exists (select 1 from public.topic_memberships tm where tm.topic_id = p.topic_id and tm.user_id = (select auth.uid())))
  )
);
create policy comments_update_author on public.comments for update to authenticated
using (author_id = (select auth.uid()))
with check (author_id = (select auth.uid()));
create policy comments_delete_author on public.comments for delete to authenticated
using (author_id = (select auth.uid()));

revoke all on public.topics, public.topic_memberships, public.posts, public.comments from anon, authenticated;
grant select on public.topics, public.posts, public.comments to anon, authenticated;
grant select, insert, delete on public.topic_memberships to authenticated;
grant insert, delete on public.posts, public.comments to authenticated;
grant update (body) on public.posts, public.comments to authenticated;
grant select, insert, update, delete on public.topics, public.topic_memberships, public.posts, public.comments to service_role;

revoke all on function public.attribute_post_author() from public, anon, authenticated;
revoke all on function public.attribute_comment_author() from public, anon, authenticated;
