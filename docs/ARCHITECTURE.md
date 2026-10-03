# Architecture outline

Status: native Android foundation implemented; backend components remain proposed. See [ADR-0010](decisions/ADR-0010-native-android-foundation.md).

The client uses Kotlin, Jetpack Compose and Material 3, minSdk 26 and compile/targetSdk 36. A local ProjectRepository supplies fictional demo data. Saved Compose state retains navigation and filters. No network or authentication is implemented.

## Components

- Android client: UI, local cache, drafts, authenticated requests, notifications.
- Backend API: authentication integration, access control, project/community operations, messaging, search and moderation.
- Database: persistent entities and relationships with migrations.
- Media storage: authorised upload and delivery, quotas and validation.
- Background jobs: notifications, media processing and retention tasks.
- Administration interface: restricted moderation and operational controls; implementation form remains open.

## Principles

Enforce permissions on the server. Clients cannot choose their own roles or project ownership. Keep API contracts independent of Android UI so a future contributor can implement iOS. Use pagination and deliberate cache invalidation. Keep production and development data separate. Record operational failures without logging tokens or private message bodies.

## Decisions still needed

identity provider and registration method; backend language/framework; database and hosting; notification transport; media quotas; search approach; distribution channel and cost; supported device test matrix; licensing; observability and backup objectives.

No framework, vendor, or paid service is committed by this outline. Create ADRs when choices are made. Publish API operations, schemas, errors, permission rules, pagination and compatibility expectations before client/backend integration.
