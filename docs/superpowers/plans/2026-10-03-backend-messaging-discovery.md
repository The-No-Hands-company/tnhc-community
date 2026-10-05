# Backend Messaging, Notifications, and Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add private participant-only messaging, Android push notifications with member preferences, and privacy-safe search for TNHC projects and public community content.

**Architecture:** PostgreSQL stores conversation membership, messages, notification outbox, device tokens and search indexes. Realtime delivers authorized updates; a server-side scheduled invocation of an idempotent outbox Edge Function sends generic FCM hints. Search uses PostgreSQL indexes, never private tables.

**Tech Stack:** Supabase/PostgreSQL/Auth/RLS/Realtime/Edge Functions, Firebase Cloud Messaging Android, Kotlin/Compose, SQL/pgTAP.

**Spec:** [Backend architecture plan](../../BACKEND_ARCHITECTURE.md), private communication, push, search, authorization, and release plan.

## Global Constraints

- Target alpha 0.0.5; product version stays 0.0.0 until release.
- Accounts, content and trust plans pass first.
- Only current conversation participants can read messages; blocks prohibit new contact and suppress notifications.
- Push payloads contain no message body, report details, or private content; app refetches with current authorization.
- Message retries use an idempotency key and never create duplicate records.
- Search excludes private conversations, reports, hidden profiles and blocked content.

---

### Task 1: Add private conversations and idempotent message storage

**Files:**
- Create: `backend/supabase/migrations/20261003000400_messaging.sql`
- Create: `backend/supabase/tests/database/messaging.test.sql`
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Create: `app/src/main/java/com/tnhc/community/data/Conversation.kt`

**Interfaces:** Tables `conversations(id, created_by, created_at, closed_at)`, `conversation_participants(conversation_id, user_id, joined_at, left_at)`, and `messages(id, conversation_id, sender_id, idempotency_key, body, created_at, deleted_at)` with unique `(sender_id, idempotency_key)`. RPC `send_message(conversation_id, idempotency_key, body)` returns the existing or newly inserted message.

- [ ] Write SQL tests for participant select/send, nonparticipant denial, removed participant denial, blocked pair denial, length limits, and duplicate idempotency key returning one message.
- [ ] Run `supabase test db`; expected: tests fail before schema and RPC exist.
- [ ] Create private RLS policies, atomic membership/block checks, unique idempotency constraint, bounded message body, and safe conversation creation rules.
- [ ] Run `supabase db reset && supabase test db`; expected: all isolation and duplicate-send tests pass.
- [ ] Add repository transport tests for successful send, duplicate retry, offline failure and conversation cursor pagination.
- [ ] Connect Messages tab to real conversation list/detail with explicit sent/failed states; never claim a message sent before server acknowledgement.

### Task 2: Add realtime delivery and notification outbox

**Files:**
- Create: `backend/supabase/migrations/20261003000410_notification_outbox.sql`
- Create: `backend/supabase/functions/dispatch-notifications/index.ts`
- Create: `backend/supabase/tests/functions/dispatch-notifications.test.ts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/tnhc/community/notifications/CommunityMessagingService.kt`
- Create: `app/src/main/java/com/tnhc/community/notifications/NotificationRepository.kt`

**Interfaces:** Tables `notification_preferences(user_id, category, enabled)`, `device_tokens(id, user_id, token_hash, platform, updated_at, disabled_at)`, `notification_outbox(id, event_type, recipient_id, target_id, attempts, available_at, delivered_at, last_error_code)`, and `notifications(id, recipient_id, target_type, target_id, read_at, created_at)`. Sender interface `PushSender.send(token, data): PushResult` is injected for tests; production adapter reads FCM credentials only from server secret storage.

- [ ] Write function tests for preference-off, blocked sender, invalid token, transient FCM failure/retry, duplicate event, and payload contains only non-sensitive IDs/type.
- [ ] Run function tests; expected: sender/outbox tests fail before implementation.
- [ ] Insert outbox events transactionally with messages and relevant activity. Dispatch idempotently, use exponential retry/backoff, stop retrying invalid tokens, and never log token/payload bodies.
- [ ] Add a host-side systemd timer (or cron if systemd is unavailable) that invokes the dispatch function over localhost once per minute with a server-only secret; test that an unauthenticated/public invocation cannot dispatch jobs.
- [ ] Add FCM Android registration that stores/refreshes token through authenticated API, handles sign-out token removal, and respects notification permissions/preferences.
- [ ] Subscribe to participant-authorized Realtime message changes; fall back to refresh on reconnect and ensure no message data is displayed from untrusted notification text.
- [ ] Run database/function tests and Android unit tests; expected: duplicates, block, preference and retry cases pass.

### Task 3: Add privacy-safe PostgreSQL search

**Files:**
- Create: `backend/supabase/migrations/20261003000420_search.sql`
- Create: `backend/supabase/tests/database/search.test.sql`
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/HomeScreen.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/CommunityApp.kt`

**Interfaces:** RPC `search_community(query, category, cursor, page_size)` returns only `result_type`, `id`, `title`, `summary`, `owner_label`, `updated_at`, and `cursor`. Searchable sources are visible projects, visible topics, and approved public posts; page size maximum is 50.

- [ ] Write pgTAP tests for case-insensitive queries, empty query rejection, bounded results, cursor stability, hidden project exclusion, private post exclusion, report/message exclusion, and block filtering.
- [ ] Run `supabase test db`; expected: tests fail before index/function exists.
- [ ] Add GIN full-text indexes and a SECURITY INVOKER search function with explicit source allow-list and bounded page size.
- [ ] Implement Android search debounce, query cancellation, no-results state, error/retry, and typed result navigation.
- [ ] Run `supabase db reset && supabase test db` and `./gradlew :app:testDebugUnitTest`; expected: search privacy and UI tests pass.

### Task 4: Verify communication/discovery and release gate

**Files:**
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`
- Modify: `docs/TESTING.md`, `docs/SECURITY_AND_MODERATION.md`
- Modify: `FEATURES.md`, `features.json`, `features.csv`, `ROADMAP.md`
- Create: `docs/development/0.0.5-messaging-discovery.md`

**Interfaces:** Push is a hint whose target must pass a fresh authorized fetch; message histories are cursor-paginated and participant-only.

- [ ] Run two-user device tests for participant message, nonparticipant denial, idempotent retry, block suppression, preference suppression, read state, notification deep link authorization and search exclusion.
- [ ] Run `supabase db reset && supabase test db` and `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`; expected: all pass.
- [ ] Record that FCM requires Google Play services and test app behavior with notifications denied/unavailable.
- [ ] Update synchronized feature exports and release evidence; do not claim messaging or push delivered before end-to-end checks pass.
