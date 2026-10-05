# Architecture outline

Status: native Android foundation implemented; the client now includes invite-only sign-in, editable profiles, password setup/change, sign-out and project follow controls behind the self-hosted backend configuration. Local-backend/device integration is verified; public deployment prerequisites remain. See [ADR-0010](decisions/ADR-0010-native-android-foundation.md), [ADR-0011](decisions/ADR-0011-self-hosted-backend.md), [ADR-0012](decisions/ADR-0012-android-supabase-client.md), and the [backend architecture plan](BACKEND_ARCHITECTURE.md).

The client uses Kotlin, Jetpack Compose and Material 3, minSdk 26 and compile/targetSdk 36. A local ProjectRepository supplies fictional demo data. Saved Compose state retains navigation and filters. No network or authentication is implemented.

## Components

- Android client: UI, local cache, drafts, authenticated requests, notifications.
- Backend platform (local accounts/follows foundation implemented; later domains planned): self-hosted Supabase on the existing machine, with PostgreSQL, Auth, PostgREST, Storage, Realtime and narrowly scoped Edge Functions.
- Database: PostgreSQL as source of truth; versioned migrations, row-level security, indexed search and explicit deletion/retention rules.
- Media storage: Supabase Storage with content ownership, access rules, quotas and validation.
- Background jobs: transactional notification outbox, FCM delivery, retries and retention tasks.
- Administration: Studio and operational tools restricted to local/private management access; member moderation tools are permission-scoped.

## Principles

Enforce permissions on the server. Clients cannot choose their own roles or project ownership. Keep API contracts independent of Android UI so a future contributor can implement iOS. Use pagination and deliberate cache invalidation. Keep production and development data separate. Record operational failures without logging tokens or private message bodies.

## Decisions and prerequisites still needed

The platform and invite-only pilot design are accepted. Verify machine capacity, public HTTPS hostname and firewall, email delivery, separate backup destination, media quotas, retention policy, observability, and recovery objectives before deployment or beta. See [BACKEND_ARCHITECTURE.md](BACKEND_ARCHITECTURE.md).

Publish API operations, schemas, errors, permission rules, pagination and compatibility expectations before client/backend integration. A zero additional monthly spend is a target, not an assumption that domains, email, backup or utility costs are free.
