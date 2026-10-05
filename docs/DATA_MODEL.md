# Data model

The core 0.0.2 schema is implemented in
[`backend/supabase/migrations/20261003000100_core_identity_projects.sql`](../backend/supabase/migrations/20261003000100_core_identity_projects.sql).
The 0.0.3 community-content foundation is implemented in
[`backend/supabase/migrations/20261003000200_community_content.sql`](../backend/supabase/migrations/20261003000200_community_content.sql).
This file records the conceptual model for implemented and planned domains;
planned rows are not yet database tables.

| Entity | Key information and relationships |
|---|---|
| Auth user / Profile (implemented) | Auth UUID, unique lowercase handle, display name, bio, interests, visibility and account state; profile ID references `auth.users` |
| Platform role (implemented) | User, administrator/moderator/Founder role, grantor and timestamp; clients cannot assign roles; only one operator-provisioned Founder exists |
| Project (implemented) | UUID, unique slug, owner category, title, summary, stage, tags and visibility |
| Project membership (implemented) | Project, user and owner/maintainer/contributor/tester role; unique project/user pair |
| Follow (implemented) | Project supplied by the caller; owner UUID defaults from `auth.uid()`; unique pair, private to the follower |
| Invitation (implemented) | Normalized email, inviter, expiry, accepted timestamp and Auth user reference; confirmation secrets remain in Supabase Auth |
| Post (implemented) | Auth-attributed author, exactly one project/topic context, bounded text, inherited visibility and moderation state |
| Comment (implemented) | Auth-attributed author, post, bounded text and moderation state |
| Topic (implemented) | Shared subject, description and public/member/private visibility |
| Topic membership (implemented) | Topic and member UUID; public/member topics are self-joinable by active accounts; private memberships are server-managed |
| Opportunity | Project, author, type, requirements, compensation description, open/closed state |
| Conversation / Participant | Participants and membership state |
| Message | Conversation, sender, content, delivery timestamp, moderation/deletion state |
| Notification | Recipient, type, target, read state |
| Block | Blocking user and blocked user; unique pair |
| Report / ModerationAction | Restricted reporter, subject, reason, reviewer, action and audit times |
| MediaAsset | Owner, storage reference, type, size, validation status, access scope |
| Listing (later) | Seller, product/service, description, availability; separate from payments |

Use stable identifiers and timestamps. Enforce ownership, relationship uniqueness, valid stages, and deletion behavior. Never use public profile handles as immutable database keys. Credentials belong to an appropriate authentication system, not public profile records.

Posts inherit the visibility of their single project or topic context. Project
posts require project membership; posts under official projects require the
author to be an owner or maintainer. Topic posts require topic membership.
Comments follow the parent post's access rules. Authors can edit and delete
their own posts and comments; moderation state remains server-controlled.
Hidden or removed content is excluded from public reads.

The deployed meanings of project visibility are: `public` is readable by
anyone, `members` by active members, and `private` by explicitly assigned
project members. Profile visibility is `members` or `private`; active members
can see member-visible profiles, while a private profile is owner-only.

Migration records require an ID, forward changes, impact, backup/recovery
plan, and affected releases. Account deletion must explicitly address authored
posts, messages, reports, media and legally required retention; settle policy
before beta.
