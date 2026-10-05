# Backend architecture plan

Status: the accounts/follows foundation is implemented and verified against the local Supabase stack for alpha 0.0.2. Later backend domains and public deployment remain planned. The architecture decision is recorded in [ADR-0011](decisions/ADR-0011-self-hosted-backend.md). Product version remains 0.0.0; no public service or live host is claimed.

## Goals and boundaries

Provide one durable, permission-checked source of truth for Android and any future iOS client. Cover accounts, TNHC projects, member activity, collaboration, messaging, moderation, media, notifications and search. Keep payments, marketplace checkout, events, video calls and member project publishing outside the first backend releases.

The backend will use an existing always-on machine and internet connection to target zero additional monthly hosting spend. That does not make operation cost-free: TNHC owns power, network, updates, security, monitoring, backups, recovery and support. Public pilot readiness depends on verifying machine capacity, a stable HTTPS hostname, invitation/recovery email delivery, and off-machine backup storage. If any of these require spending, public access waits for an explicit budget decision.

## Proposed deployment

Self-host Supabase with its supported Docker Compose deployment. It supplies:

- PostgreSQL as the authoritative relational data store.
- Supabase Auth for identity, sessions, invitation and recovery workflows.
- PostgREST for database-backed REST access and generated API descriptions.
- Supabase Storage for project, profile and post media.
- Realtime for authorized conversation updates and selected live community changes.
- Edge Functions for privileged workflows such as issuing invitations, applying moderation actions and dispatching push notifications.

Use PostgreSQL migrations as the versioned schema and policy history. Keep Supabase service secrets only on the server. The Android APK contains only public client configuration and a publishable key; no database password, service-role key or signing secret is ever shipped in it. Android and future iOS clients use the same Auth, REST/RPC, Storage and Realtime interfaces. Publish a stable API contract and compatibility rules before client/backend integration.

Use a local Supabase CLI stack for development and automated tests. Keep test data separate from a closed pilot. Treat any externally reachable instance as production data: there is no casual shared development database. Pin the self-hosted release and image versions, test upgrades against a restored copy, and record each schema change as a migration.

## Backend domains

| Domain | Persistent records and behavior | Target |
|---|---|---|
| Identity and membership | Auth identity, public profile, interests, account state, invitation records, platform and scoped project roles | 0.0.2 |
| Project catalogue | Official TNHC, community and partner projects; stage, tags, maintainers, visibility, links and media metadata | 0.0.2; verified catalogue import separately |
| Follows and feed inputs | Unique member/project follows; feed reads from followed projects and shared topics | 0.0.2 |
| Participation | Project updates, comments and topic discussions, attributed to their authors and paginated | 0.0.3 |
| Collaboration | Opportunities, skills/terms, open/closed state and member responses | 0.0.4 |
| Trust and moderation | Blocks, restricted reports, moderation queue, recorded actions, reasons and appeal/support references | 0.0.4 |
| Private communication | Conversation membership, messages, delivery state and idempotent sends | 0.0.5 |
| Notifications | Device push tokens, preferences, unread state, delivery attempts and authorized target references | 0.0.5 |
| Search | PostgreSQL full-text and structured filters for projects, topics and permitted public community content | 0.0.5 |
| Media | Metadata, ownership, visibility, validation state, size/type limits and storage key | Added with the first feature that needs uploads |
| Marketplace | Listings and enquiries only; seller rules and payment processing require a separate decision | Later than 1.0.0 |

The conceptual entities and relationships are maintained in [DATA_MODEL.md](DATA_MODEL.md). Use stable UUIDs, UTC timestamps, uniqueness constraints for relationship pairs, foreign keys, and cursor pagination for changing feeds and message history. Credentials remain in the authentication provider, not profile rows. Do not store payment credentials.

The Founder uses a private Android build with a server-owned single-holder role.
The flavor exposes Founder controls only after the backend confirms the signed-in
account. It does not grant access. Provisioning and recovery use a private
database operator connection; Founder actions use narrow RLS-checked operations
and append-only audit entries. The Founder can manage in-app community records
and settings as those domains ship. Infrastructure operations and general
access to private message bodies remain outside the app control plane.

## API and client contract

Use a single HTTPS origin and the Supabase `/auth/v1`, `/rest/v1`, `/storage/v1`, `/realtime/v1` and `/functions/v1` interfaces behind the API gateway. `/rest/v1` is PostgREST's API endpoint; the product contract is tracked separately through migrations and operation documentation, with additive-compatible changes preferred and breaking changes given a new exposed contract. Schema/table exposure is allow-listed. Domain mutations that need multiple writes or elevated privileges run as narrowly scoped SQL functions or Edge Functions and are callable only by their intended role. Do not let clients submit role names or trusted ownership fields.

Before integration, document for each operation: request and response fields, validation, authentication requirement, authorization rule, pagination, idempotency behavior, error codes and compatibility. Prefer opaque cursor pagination, bounded page sizes, and idempotency keys for message sends and other retry-sensitive writes. Keep database errors and secrets out of user-facing responses.

## Authorization and privacy

PostgreSQL row-level security is the final access boundary for every exposed table and storage object. Deny by default, then add policies for the precise member, maintainer, moderator or administrator capability. Add automated tests that attempt both allowed and forbidden operations using separate accounts and unauthenticated requests.

- A member may edit only their own profile, interests, preferences and device tokens.
- Project maintainers may edit only projects to which they are assigned; official updates require that scoped permission.
- Reports are visible only to the reporter for status and to authorized moderators/admins for review. A report may include only the specific content needed to assess it.
- A moderator does not receive general access to private conversations. If a message is reported, only the reported message and tightly bounded context may be available under a documented review rule.
- Conversation reads and sends require active participant membership. A block prevents new conversations, messages and unwanted contact notifications in either direction as specified by policy.
- Notifications contain no private message body or sensitive report details. Opening a notification performs a fresh authorization check.
- Storage access follows the owning content's visibility. Uploads have strict size/type limits, server-verified content types, generated object keys, and no client-selected public ACLs.
- Role grants, invite use, moderation actions and account-state changes are auditable. Logs exclude access tokens, passwords, private message bodies and report evidence.

Account deletion, retained authorship, message treatment, report retention, and appeal retention need a written policy before beta. Apply deletion consistently across relational records, storage objects, search indexes and push tokens.

## Workflows

### Invite-only account creation

Open self-registration is disabled for the pilot. An administrator or authorized invitation function issues a single-use, expiring invitation. The recipient establishes an Auth account through the invitation flow; the server records the accepted invitation and creates the member profile with a least-privileged role. Invitations and account recovery require a configured transactional email path. Until that path is available and tested, use non-public test accounts only; do not expose manual admin provisioning as a public flow.

### Posts and media

Client uploads are authorized against the member and target project/topic. Store media metadata in PostgreSQL and bytes in Storage. Validate size and decoded content, reject unsupported types, and bind each asset to an owner and visibility scope. Feed and comment endpoints use cursor pagination and enforce the same RLS rules as detail reads.

### Messaging and push

Persist a message and a notification-outbox record in one database transaction. A retry with the same idempotency key returns the existing message rather than inserting another. A background worker/function dispatches pending events to Firebase Cloud Messaging (FCM), retries transient failures, and retires invalid device tokens. Push is a hint only; the client fetches the message through participant-authorized APIs. Respect member preferences and blocks before enqueue and again before delivery where practical. Push transport depends on Google Play services; the app must still show messages when opened if push is delayed or unavailable.

### Search and discovery

Start with PostgreSQL indexes and full-text search over explicitly public/discoverable fields. Never include private conversations, reports, hidden profiles or blocked content in general search. Add an external search service only if measured catalogue size or query performance later requires it.

## Network and operations

- Serve clients only over HTTPS with a valid certificate. Confirm DNS/hostname and certificate renewal before pilot onboarding.
- Expose the HTTPS gateway only. Keep PostgreSQL ports, Docker management, Supabase Studio and administrative dashboards off the public internet; access administration locally or through a private management network.
- Use host firewall rules, strong generated secrets, least-privilege credentials, operating-system updates, pinned container releases and rate limits for sign-in, invitations, writes and uploads.
- Separate database and media backups from the machine hosting them. Encrypt backups, restrict access, set retention, and test a full database-and-media restore to a clean environment before beta. A backup on the same disk is not sufficient.
- Monitor service health, disk capacity, backup age, job failures and certificate expiry. Logs must be privacy-minimized. Define who responds to outages and account abuse before an external pilot.
- Record recovery-point and recovery-time objectives before beta. Home-hosting has one-machine, power and internet failure modes; describe those limitations to pilot members and move to a managed host if the agreed availability cannot be met.

Supabase self-hosting is community-supported and makes TNHC responsible for maintenance, hardening, database operations, uptime and disaster recovery. Its documentation sets a minimum of 4 GB RAM and a 40 GB SSD for all components, recommends 8 GB RAM and 80 GB SSD, and distinguishes the local development stack from production self-hosting. See [self-hosting responsibilities](https://supabase.com/docs/guides/self-hosting) and [Docker deployment requirements](https://supabase.com/docs/guides/self-hosting/docker).

## Release plan and gates

| Version | Backend increment | Required evidence |
|---|---|---|
| 0.0.2 | Local migrations, Auth integration, invite-only test accounts, profiles, roles, project catalogue API and follows | Two-account isolation tests; role escalation denied; migration applies from empty database; Android uses live test backend without secrets in APK |
| 0.0.3 | Updates, comments, topics, feed reads and media upload foundation | Maintainer/author permissions, pagination, invalid media rejection and private object access tests |
| 0.0.4 | Opportunities, block/report workflows and moderator queue | Block behavior end to end; report visibility restricted; moderation audit and external-pilot trust gate |
| 0.0.5 | Conversations, idempotent message writes, preferences, FCM outbox and search | Participant-only message access; retry does not duplicate; block/prefs suppress contact alerts; search excludes private records |
| 0.1.0 | Account recovery and deletion, retention jobs, operating runbook, tested off-machine restore, support route | Restore drill; deletion/retention verification; incident and rollback exercise; closed pilot approval |

No milestone is released merely because a schema or screen exists. Keep target version separate from actual introduced version in [FEATURES.md](../FEATURES.md), `features.json` and `features.csv`.

## Decisions and prerequisites still open

The platform choice, zero-monthly-spend target, existing-machine hosting path, and invite-only pilot are accepted. Before deployment work, verify:

1. Server operating system, Docker support, available memory, SSD capacity, backup disk and sustained 24/7 operation.
2. Public HTTPS hostname, DNS control, router firewall rules and stable inbound reachability.
3. SMTP/email service for invitations and account recovery; account email must arrive reliably.
4. Separate backup destination and agreed retention/recovery objectives.
5. Exact self-hosted release, secret rotation, monitoring/alerting, and operator/support ownership.
6. Data retention and deletion rules, notification consent, media quotas, and jurisdiction/store disclosures before beta.

If these prerequisites reveal unavoidable spend, stop before incurring it and present the cost and options for a decision. The zero-spend design target is not authorization to purchase a domain, server, backup service or email plan.
