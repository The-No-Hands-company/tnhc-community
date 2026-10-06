# Project Catalogue and Communities Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Populate TNHC Community with 32 code-bearing Nexus projects, the released DevTrack project, real public topic communities, and a sourced launch update.

**Architecture:** Supabase remains the source of truth. A forward migration adds project links and the explicit `in_development` status, upserts the curated catalogue/topics, and imports one real release update. The Android repositories and Compose screens read and write through existing Supabase APIs; RLS and database triggers continue to enforce access and authorship.

**Tech Stack:** Kotlin, Jetpack Compose, Supabase Kotlin/PostgREST, PostgreSQL migrations, pgTAP, Gradle.

**Spec:** [Project catalogue and communities design](../specs/2026-10-05-project-catalog-and-communities.md)

## Global Constraints

- Use only the approved 32 code-bearing Nexus entries and DevTrack 1.0; exclude scaffold/stub, personal test/research, and upstream projects.
- Label the Nexus register's project status source date as 2026-08-19; label DevTrack's public release date as 2026-10-02.
- Never create synthetic member profiles or fabricated engagement.
- Never put a Supabase service-role key or SMTP credential in the Android app.
- All topic, post, project, and membership authorization remains enforced by Supabase RLS/functions.

---

### Task 1: Extend project source metadata and status

**Files:**
- Create: `backend/supabase/migrations/20261005000100_project_sources_and_development_stage.sql`
- Modify: `backend/supabase/tests/database/backend_baseline.test.sql`
- Modify: `backend/supabase/tests/database/founder_catalog_controls.test.sql`
- Modify: `backend/supabase/migrations/20261003000220_founder_catalog_controls.sql` through a forward replacement migration only; do not rewrite an applied migration
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Modify: `app/src/main/java/com/tnhc/community/data/Project.kt`
- Modify: `app/src/founder/java/com/tnhc/community/data/FounderRepository.kt`
- Modify: `app/src/founder/java/com/tnhc/community/ui/FounderConsole.kt`

**Interfaces:** Public `Project` adds nullable `websiteUrl` and `repositoryUrl`; `Project` keeps the server stage string. `FounderProject` and `FounderProjectDraft` carry both URLs. Existing repository callers remain source-compatible by defaulting draft URLs to null.

- [ ] Add database assertions that only HTTP(S) URLs are accepted and `in_development` is a supported stage.
- [ ] Run the database tests and confirm the new assertions fail on the current schema.
- [ ] Add nullable URL columns and replace the stage check in the new migration; extend `founder_upsert_project` and `founder_list_projects` with those fields and URL validation.
- [ ] Update the public project RPC, Kotlin serialization records, and model mapping to preserve URLs and stage labels.
- [ ] Add Founder URL fields to project create/edit forms and pass them through the server-checked RPC.
- [ ] Run database and JVM tests.

### Task 2: Add the reviewed production catalogue and topics

**Files:**
- Create: `backend/supabase/migrations/20261005000200_initial_project_catalogue_and_topics.sql`
- Modify: `backend/supabase/tests/database/backend_baseline.test.sql`
- Modify: `backend/supabase/tests/database/community_content.test.sql`
- Modify: `docs/DATA_MODEL.md`

**Interfaces:** Migration is idempotent by project slug and topic slug. It creates 33 projects (32 official Nexus entries plus member-owned DevTrack), five public topics, Founder ownership memberships for official projects, and one Founder-authored DevTrack release post linked to the source article and release page.

- [ ] Add test assertions for 33 stable slugs, 6 `released`, 1 `beta`, 25 `in_development`, 1 member-owned DevTrack entry, five public topics, and one sourced post.
- [ ] Run the database tests and confirm missing catalogue rows fail.
- [ ] Insert/upsert the approved records with factual descriptions and repository/source links; set only the source-supported DevTrack release date.
- [ ] Upsert the five public topics: Start Here, Nexus & Self-Hosting, Developer Tools, Creative Projects, and Project Releases.
- [ ] Insert the DevTrack release update only for the provisioned Founder account; fail safely if no Founder exists.
- [ ] Run the database suite and check migration retry behavior.

### Task 3: Add community read and membership repository operations

**Files:**
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Modify: `app/src/test/java/com/tnhc/community/data/CommunityRepositoryTest.kt`

**Interfaces:** Add `CommunityTopic(id, slug, title, description, visibility)`, `CommunityPost(id, authorId, topicId, body, createdAt)`, and repository methods `loadTopics()`, `loadJoinedTopicIds()`, `setTopicMembership(topicId, joined)`, `loadTopicPosts(topicId)`, and `createTopicPost(topicId, body)`.

- [ ] Add unit tests for trimmed non-empty post bodies up to 5000 characters, blank topic IDs, membership changes, and pass-through of topic/post query results.
- [ ] Run the targeted unit tests and verify they fail before implementation.
- [ ] Implement PostgREST reads and writes using only public topic access and the existing RLS-protected membership/post tables.
- [ ] Require an authenticated session for membership and posting; do not accept a caller-supplied author ID.
- [ ] Run targeted unit tests and compile both product flavors.

### Task 4: Replace the Community placeholder with a usable topic feed

**Files:**
- Create: `app/src/main/java/com/tnhc/community/ui/CommunityScreen.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/CommunityApp.kt`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`

**Interfaces:** `CommunityScreen` receives the repository, current session state, and a sign-in navigation callback. It displays topic cards, joined state, a selected topic's visible posts, a post composer for joined active members, and explicit loading/empty/error recovery.

- [ ] Add UI tests for loaded topics, anonymous read-only view, join/leave, post submission, empty feed, retry, and failed write feedback.
- [ ] Run the targeted device tests and confirm the old placeholder behavior fails the new expectations.
- [ ] Implement the screen with accessible labels and disabled repeat-submit controls while a request is in flight.
- [ ] Replace only the Community destination placeholder; preserve the other destinations.
- [ ] Run Member and Founder Android tests on an emulator/device.

### Task 5: Surface project links and verify the populated experience

**Files:**
- Modify: `app/src/main/java/com/tnhc/community/ui/ProjectScreens.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/HomeScreen.kt`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`
- Modify: `docs/TESTING.md`
- Modify: `FEATURES.md`, `features.json`, and `features.csv` together

- [ ] Add UI tests that a project with a website/repository URL shows labeled actions and a project without URLs shows neither action.
- [ ] Add project link actions with Android `LocalUriHandler` and display the exact lifecycle labels Released, Beta, and In development.
- [ ] Put released projects before beta and in-development records in catalogue ordering; keep stable title/slug tie-breaking.
- [ ] Run `git diff --check`, database tests, `:app:testMemberDebugUnitTest`, `:app:testFounderDebugUnitTest`, `:app:assembleMemberDebug`, and `:app:assembleFounderDebug`.
- [ ] Deploy migrations and edge functions only after all code checks pass, then verify production record counts and the app screens on the connected phone.
