# Changelog

## Unreleased — alpha 0.0.1 target

- Added native Kotlin/Compose Android foundation with Home, Projects, Community, Messages and Profile.
- Added four labelled fictional demo projects, local text/category filtering, project details, empty/error recovery and saved navigation state.
- Added unit and Android UI tests, Gradle wrapper, CI build workflow, stack decision and developer setup instructions.
- Updated documentation to DOC-0002 and feature tracking to in_review. No accounts, messaging, project import or release is claimed.
- Added DOC-0003 backend architecture and self-hosting decision; it records the design approved before implementation began.
- Added DOC-0004 staged backend implementation plan covering accounts, community content, trust, messaging/discovery, and operations; alpha 0.0.2 execution is recorded below.

## Unreleased — alpha 0.0.2 target

- Added DOC-0006 for accounts/follows implementation evidence and DOC-0007 for verified local integration. Refreshed the debug APK checksum.
- Added the local Supabase stack, invite-only account lifecycle, identity/project/follow schema, row-level policies, cursor RPC, and administrator invitation function.
- Added the Android Supabase adapter, encrypted Auth session storage, invite deep-link handling, and optional live project-catalogue loading.
- Added invite-only sign-in; profile editing with member/private visibility; password setup/change and sign-out; and persistent project follow/unfollow controls with retry-safe feedback.
- Added Compose and API 36 emulator coverage for invite-only access, profile save/password/sign-out, follow/unfollow, cross-account catalogue isolation, failed logout, and process-restored invitation sessions.
- The API contract, 64 PostgreSQL assertions, eight invitation tests, and Android unit, lint, connected-emulator, and Auth invitation/restart checks are recorded in the 0.0.2 development record. This target is not released and no public backend is deployed.


Record actual changes under Added, Changed, Fixed, Removed, Security, and Known limitations as applicable. Planned features belong in ROADMAP.md.

### Fixed

- Granted the invitation function narrowly scoped access to invitation records and sent Auth return URLs as `redirect_to` query parameters. Offline/cancelled sign-out now removes local sessions, and the catalogue reloads on account changes.

## Unreleased — product 0.0.0

### Added

- DOC-0001 (2026-10-03): initial Android-first product documentation, staged version policy, planned roadmap, feature register, tracking exports, and release templates.

### Known limitations

- No public backend or DevTrack integration is delivered. Local accounts/follows implementation is verified; public deployment prerequisites and DevTrack integration remain open.
- Distribution, final application ID, license, email delivery, hostname and backup destination remain open.

## Release entry template

Copy only when an actual release is made:

`## [X.Y.Z] — YYYY-MM-DD`

List feature IDs, delivered behavior, fixes, migrations, verified checks and known limitations. Link the corresponding release record and immutable Git tag.
