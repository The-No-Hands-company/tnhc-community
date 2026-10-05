# Backend Community Content and Media Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let authenticated members follow TNHC project updates, comment, and participate in shared topic communities with controlled media uploads.

**Architecture:** Extend the existing PostgreSQL/Auth/PostgREST backend with migration-owned content tables and RLS. Store media bytes in Supabase Storage and metadata/ownership in PostgreSQL. Android consumes paginated APIs through the repository boundary created in the 0.0.2 plan.

**Tech Stack:** Supabase/PostgreSQL, SQL/pgTAP, Supabase Storage, Kotlin/Compose and existing repository abstractions.

**Spec:** [Backend architecture plan](../../BACKEND_ARCHITECTURE.md), backend domains, authorization, posts/media workflows, and release plan.

## Global Constraints

- Target alpha 0.0.3; product version stays 0.0.0 until release.
- Accounts/follows plan (TNHC-003–005) passes first; use the established local Supabase stack.
- RLS must enforce access for table queries and Storage objects; client-supplied ownership is ignored.
- Pagination is bounded and cursor-based; private data never enters public search/feed projections.
- No private message, service-role key, or database secret may be logged or shipped in the APK.

---

### Task 1: Add topic, post, comment, and membership migrations

**Files:**
- Create: `backend/supabase/migrations/20261003000200_community_content.sql`
- Create: `backend/supabase/tests/database/community_content.test.sql`
- Modify: `backend/supabase/seed.sql`
- Modify: `docs/DATA_MODEL.md`

**Interfaces:** Tables: `topics(id, slug, title, description, visibility, created_at)`, `topic_memberships(topic_id, user_id, created_at)`, `posts(id, author_id, project_id, topic_id, body, visibility, moderation_state, created_at, updated_at)`, and `comments(id, post_id, author_id, body, moderation_state, created_at, updated_at)`. Require at least one valid project/topic context according to the spec's content rules.

- [ ] Write pgTAP tests for valid/invalid context, author attribution, foreign keys, bounded text fields, indexes, RLS, and author-only edit/delete behavior.
- [ ] Run `supabase test db`; expected: tests fail before the migration.
- [ ] Create constraints, indexes, and RLS policies. Public/discoverable reads follow visibility and moderation state; authors edit their own content; official update creation requires active project maintainer membership.
- [ ] Add synthetic topic/project posts to local seed data only.
- [ ] Run `supabase db reset && supabase test db`; expected: all migration and content-policy tests pass.
- [ ] Commit migration and tests together.

### Task 2: Add bounded feed and comment APIs

**Files:**
- Create: `backend/supabase/migrations/20261003000210_content_feed_functions.sql`
- Create: `backend/supabase/tests/database/content_feed.test.sql`
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Create: `app/src/main/java/com/tnhc/community/data/Post.kt`
- Create: `app/src/main/java/com/tnhc/community/ui/FeedViewModel.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/CommunityApp.kt`

**Interfaces:** Repository exposes `loadFeed(cursor: String?, pageSize: Int)`, `publishPost(projectId: UUID?, topicId: UUID?, body: String)`, `loadComments(postId: UUID, cursor: String?, pageSize: Int)`, and `addComment(postId: UUID, body: String)`. Server caps page size at 50 and returns opaque next cursor.

- [ ] Write SQL tests for cursor ordering with equal timestamps, max page size, visibility filtering, and blocked/hidden author exclusion.
- [ ] Run `supabase test db`; expected: feed tests fail while functions do not exist.
- [ ] Implement stable ordering by `(created_at DESC, id DESC)` and a bounded cursor function; reject cursor decode/limit violations with a safe error.
- [ ] Add repository serialization and fake-transport tests for pagination, empty feed, retry, publish error and comment submission.
- [ ] Run `./gradlew :app:testDebugUnitTest`; expected: feed/repository tests pass.
- [ ] Connect the Community tab to live feed/topic data and retain loading, empty, retry and offline states; do not display demo posts as member content.

### Task 3: Add Storage ownership and media validation

**Files:**
- Create: `backend/supabase/migrations/20261003000220_content_storage_policies.sql`
- Create: `backend/supabase/tests/database/content_storage.test.sql`
- Create: `backend/supabase/functions/validate-upload/index.ts`
- Create: `backend/supabase/tests/functions/validate-upload.test.ts`
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/ProjectScreens.kt`

**Interfaces:** Bucket `community-media` is private. Metadata table `media_assets(id, owner_id, bucket, object_key, content_type, byte_size, visibility, validation_state, created_at)` binds a generated object key to one owner and content record. `validate-upload` accepts a Storage object reference and returns validation state; never trusts a client-declared MIME type alone.

- [ ] Test unauthenticated upload denial, cross-user read/delete denial, owner read, allowed formats, oversize files, MIME mismatch, and malformed images.
- [ ] Run database and function tests; expected: missing policies/function cause the new tests to fail.
- [ ] Add private bucket policy and server-verified limits. Configure accepted raster formats and maximum file size in one shared server config; create per-user/project quotas.
- [ ] Implement upload/download through short-lived authorized Storage URLs; persist asset metadata only after validation succeeds.
- [ ] Add Android upload cancellation, progress/error, retry without duplicate attachment, and loading image fallback tests.
- [ ] Run `supabase db reset && supabase test db` and `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`; expected: all pass.

### Task 4: Verify content release gate

**Files:**
- Modify: `FEATURES.md`, `features.json`, `features.csv`, `ROADMAP.md`
- Create: `docs/development/0.0.3-community-content.md`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`

**Interfaces:** The content API contract names author/visibility/moderation state explicitly and returns the same shape for Android and a future iOS client.

- [ ] Add two-account device tests for member post/comment, maintainer official update, forbidden official update, pagination and upload error recovery.
- [ ] Run `supabase db reset && supabase test db` and `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`; expected: all pass.
- [ ] Record executed evidence, synchronize three feature exports, keep introduced versions blank until release, and commit only after the gate is met.
