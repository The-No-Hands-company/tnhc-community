# Backend Accounts, Projects, and Follows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first live backend increment: invite-only accounts, member profiles, the TNHC project catalogue, and persistent follows.

**Architecture:** Develop against a local Supabase CLI stack. PostgreSQL migrations define tables and row-level security; Auth owns credentials and sessions; a least-privileged Android client uses the public API. Invitation issuance is a server-only function and open sign-up remains disabled.

**Tech Stack:** Supabase CLI, PostgreSQL, SQL/pgTAP, Supabase Auth/PostgREST/Edge Functions, Kotlin, Jetpack Compose, Gradle.

**Spec:** [Backend architecture plan](../../BACKEND_ARCHITECTURE.md), especially backend domains, API contract, authorization and invite-only account creation.

## Global Constraints

- Current product version stays 0.0.0; target this work to alpha 0.0.2.
- Invite-only account creation; do not enable public self-registration.
- Server credentials, database passwords, and service-role keys never enter source control or the APK.
- Every exposed table denies access by default and has tests for allowed and forbidden access.
- Use immutable UUID IDs, UTC timestamps, foreign keys, uniqueness constraints, and migrations for every schema change.
- The existing machine is a local cost target; do not buy or activate a paid service to complete development.

---

### Task 1: Add a reproducible local Supabase development stack

**Files:**
- Create: `backend/supabase/config.toml`
- Create: `backend/supabase/seed.sql`
- Create: `backend/README.md`
- Create: `docs/API_CONTRACT.md`
- Create: `.github/workflows/backend.yml`

**Interfaces:** Local CLI exposes Auth and PostgREST on documented local URLs; `docs/API_CONTRACT.md` defines operation payloads/errors and compatibility rules. The Android app reads a local backend URL and publishable key from untracked/local Gradle configuration. CI runs migrations and SQL tests without production credentials.

- [x] Record the required Supabase CLI and Docker versions in `backend/README.md`, then add `config.toml` with local Auth, API, database and seed settings.
- [x] Add `docs/API_CONTRACT.md` with Auth session behavior, project/profile/follow response fields, bounded cursor pagination, standard validation/authorization errors, and the rule that breaking API changes require a new exposed contract before changing the Android client.
- [x] Install the Supabase CLI from its official installation guide and pin the installed version in `backend/README.md` and CI; start with `supabase start`. Expected: all configured services become healthy and `supabase status` prints local endpoints.
- [x] Add CI steps to install the pinned CLI, start the local stack, run `supabase db reset`, execute `supabase test db`, and stop the stack even after failure.
- [x] Run the CI commands locally; the empty test runner reports `NOTESTS` as a nonzero result, so add a one-assertion baseline database test to keep the initial CI run green. Database reset and test then pass locally.
- [ ] Commit the stack and setup documentation without `.env`, database volumes or secrets.

### Task 2: Define the identity, role, profile, project, and follow schema

**Files:**
- Create: `backend/supabase/migrations/20261003000100_core_identity_projects.sql`
- Modify: `backend/supabase/seed.sql`
- Create: `backend/supabase/tests/database/core_schema.test.sql`
- Modify: `docs/DATA_MODEL.md`

**Interfaces:** Tables: `profiles(id, handle, display_name, bio, interests, visibility, account_state, created_at, updated_at)`, `platform_roles(user_id, role, granted_by, created_at)`, `projects(id, slug, owner_category, title, summary, stage, tags, visibility, created_at, updated_at)`, `project_memberships(project_id, user_id, role, created_at)`, `project_follows(project_id, user_id, created_at)`, and `invitations(id, normalized_email, invited_by, expires_at, accepted_at, auth_user_id, created_at)`. Invitation records contain no confirmation secret. IDs referencing accounts use `auth.users(id)`. Profile visibility defaults to `members` (authenticated members only); `private` means self only. Official project visibility defaults to `public`; member-only/private visibility is explicit.

- [x] Write pgTAP tests for required columns, unique handles/slugs, valid ownership/stage values, unique project/user follows, foreign keys, and RLS enabled on every public table.
- [x] Run `supabase test db`; expected: schema tests fail because the tables and policies do not exist.
- [x] Add the migration with checks, indexes, least-privilege grants, RLS enabled, and initial select/insert/update/delete policies. Allow visitor reads of public projects only, project-member reads according to visibility, member-visible profiles only to authenticated users, private profiles only to their owner, and updates only by the owning member.
- [x] Seed only synthetic test projects with explicit demo labels. Do not import the claimed 300+ projects without a verified source inventory.
- [x] Run `supabase db reset && supabase test db`; schema, constraints and two-account isolation tests pass locally.
- [ ] Commit migration, tests, seed and data-model documentation together.

### Task 3: Add invite-only Auth and profile creation

**Files:**
- Create: `backend/supabase/functions/invite-member/index.ts`
- Create: `backend/supabase/tests/functions/invite-member.test.ts`
- Modify: `backend/supabase/config.toml`
- Modify: `backend/supabase/migrations/20261003000100_core_identity_projects.sql`
- Modify: `backend/README.md`

**Interfaces:** `POST /functions/v1/invite-member` accepts `{ email }` from an authenticated administrator and returns `{ invitation_id, status }`. Store an invitation record with normalized email, creator, expiry, accepted state and Auth user ID; Supabase Auth owns the one-use confirmation token. Call Auth Admin only from the function secret environment. Auth user creation triggers a profile row whose initial handle is `member-` plus the 32 lowercase hex characters of the Auth UUID; user-supplied metadata cannot set role or account state.

- [x] Write function tests for anonymous caller, ordinary member, administrator, duplicate pending email, expired/used invite and Auth-provider failure.
- [x] Disable open sign-up in local Auth config; implement administrator checks, duplicate-pending-invite prevention, expiring invitation records, Auth Admin invitation issuance, and safe error mapping.
- [x] Add a database trigger that creates a pending profile from an Auth user ID; ignore client-supplied role/account-state metadata. Mark the profile active only after the invitation is accepted.
- [x] Run Deno unit tests and type-check the function; pgTAP tests cover invite activation and expiry. SMTP is available locally through Mailpit; external invitations require configured SMTP.
- [x] Record that email sending requires configured SMTP before external invitations; keep credentials out of the repository.

### Task 4: Connect Android authentication, profile, projects, and follows

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/tnhc/community/data/BackendConfig.kt`
- Create: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Modify: `app/src/main/java/com/tnhc/community/data/ProjectRepository.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/CommunityApp.kt`
- Create: `app/src/test/java/com/tnhc/community/data/CommunityRepositoryTest.kt`

**Interfaces:** `CommunityRepository` exposes suspend operations `signIn(email, password)`, `signOut()`, `loadProjects(cursor, filter)`, `loadProfile()`, `updateProfile(displayName, bio, interests)`, and `setProjectFollow(projectId, followed)`. The Compose layer depends on the repository interface, not Supabase types. Evaluate the documented `supabase-kt` client (community-maintained) against direct typed HTTP calls, record the choice in `ADR-0012`, pin compatible client/Ktor versions, and keep that transport behind the interface. See [Supabase Kotlin installation](https://supabase.com/docs/reference/kotlin/installing) and [client maintenance note](https://supabase.com/docs/reference/kotlin/introduction).

- [x] Write repository tests with a fake transport for authentication state, pagination cursor, follow/unfollow, followed-project loading, password update/minimum, profile normalization, network failure and session expiry; all nine tests pass.
- [ ] Run `./gradlew :app:testDebugUnitTest`; expected: new repository tests fail before implementation.
- [x] Add Internet permission, pinned client dependencies and backend configuration from local Gradle properties/environment. Release variants require HTTPS and a publishable key; no key literal is in source.
- [x] Implement encrypted Auth session handling and repository operations; connect the Android catalogue to the live, bounded RPC when configured and preserve demo/loading/empty/error/retry states otherwise.
- [x] Add invite-only sign-in, profile editing/visibility, password setup/change, sign-out and visible persistent follow/unfollow actions with sanitized failure feedback.
- [x] Run Android unit tests, debug APK build, lint and instrumentation APK compilation. Lint has zero errors. APK scan finds no service-role/database credential markers.
- [x] Add Compose instrumentation coverage for invite-only sign-in, profile/interests saving, password update, sign-out and follow/unfollow. Tests compile; execution remains pending an Android emulator image or attached device.

### Task 5: Verify authorization, compatibility, and alpha gate

**Files:**
- Modify: `backend/supabase/tests/database/core_schema.test.sql`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`
- Modify: `FEATURES.md`, `features.json`, `features.csv`, `ROADMAP.md`
- Create: `docs/development/0.0.2-backend-accounts.md`

**Interfaces:** API responses and Android repository models use stable IDs and cursor fields from the approved backend spec. Feature exports remain synchronized; introduced version stays empty until a release is actually made.

- [ ] Add two-user tests proving a member cannot edit another profile, assign roles, edit maintainer-only projects, or view private project data.
- [ ] Add retry tests proving duplicate follows cannot be created and repeated profile updates are safe.
- [ ] Run `supabase db reset && supabase test db` and the Android gates after completing profile/follow UI; device instrumentation has not run.
- [ ] Update feature statuses only to the verified state, preserve target version 0.0.2, and add test evidence to the development record.
- [ ] Commit the alpha increment only after the entire gate passes; do not deploy the public service until the Operations plan's prerequisites pass.
