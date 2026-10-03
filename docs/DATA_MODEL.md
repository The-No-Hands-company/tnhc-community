# Data model outline

These are conceptual entities, not a implemented schema.

| Entity | Key information and relationships |
|---|---|
| User / Profile | Stable ID, handle, display name, interests, skills, visibility, account state |
| RoleAssignment | User, capability/role, scope, grantor, timestamp |
| Project | Owner, ownership category, title, summary, stage, tags, visibility |
| ProjectMembership | Project, user, maintainer/contributor capability |
| Follow | User and project; unique pair |
| Post / Comment | Author, project/topic context, content, visibility, moderation state |
| Topic | Shared subject, description, moderation scope |
| Opportunity | Project, author, type, requirements, compensation description, open/closed state |
| Conversation / Participant | Participants and membership state |
| Message | Conversation, sender, content, delivery timestamp, moderation/deletion state |
| Notification | Recipient, type, target, read state |
| Block | Blocking user and blocked user; unique pair |
| Report / ModerationAction | Restricted reporter, subject, reason, reviewer, action and audit times |
| MediaAsset | Owner, storage reference, type, size, validation status, access scope |
| Listing (later) | Seller, product/service, description, availability; separate from payments |

Use stable identifiers and timestamps. Enforce ownership, relationship uniqueness, valid stages, and deletion behavior. Never use public profile handles as immutable database keys. Credentials belong to an appropriate authentication system, not public profile records.

Migration records require an ID, forward changes, impact, backup/recovery plan, and affected releases. Account deletion must explicitly address authored posts, messages, reports, media and legally required retention; settle policy before beta.
