# Backend Opportunities and Trust Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add project collaboration opportunities, user blocks, restricted reports, and an auditable moderator review queue before external community access.

**Architecture:** Store moderation and collaboration state in PostgreSQL. RLS separates ordinary member access from reporter and moderator access; narrowly scoped server functions apply moderation actions and append immutable audit records. Android exposes report/block actions and a role-gated moderator screen.

**Tech Stack:** Supabase/PostgreSQL, SQL/pgTAP, Edge Functions, Kotlin/Compose, existing backend repository.

**Spec:** [Backend architecture plan](../../BACKEND_ARCHITECTURE.md), authorization/privacy, release plan, and [security/moderation requirements](../../SECURITY_AND_MODERATION.md).

## Global Constraints

- Target alpha 0.0.4; product version remains 0.0.0 until release.
- Accounts/follows and community-content plans pass first.
- Moderators see reports and only specifically reported material; never unrestricted private conversation access.
- Block behavior is enforced in server policies and workflows, not only hidden in Android UI.
- Each moderator/role/action change is auditable and cannot be forged by a client.
- External participant access remains gated on end-to-end reporting, blocking, review, and response procedures.

---

### Task 1: Add collaboration opportunity records and APIs

**Files:**
- Create: `backend/supabase/migrations/20261003000300_opportunities.sql`
- Create: `backend/supabase/tests/database/opportunities.test.sql`
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Create: `app/src/main/java/com/tnhc/community/data/Opportunity.kt`
- Create: `app/src/main/java/com/tnhc/community/ui/OpportunitiesViewModel.kt`

**Interfaces:** Table `opportunities(id, project_id, author_id, title, description, required_skills, compensation_description, state, created_at, updated_at, closes_at)`; `opportunity_responses(id, opportunity_id, member_id, message, state, created_at)`. Maintainers create opportunities for assigned projects; members respond once; creator may close.

- [ ] Write database tests for unauthorized creation, invalid project, duplicate response, closed opportunity response, maintainer ownership, and public filtering.
- [ ] Run `supabase test db`; expected: tests fail before migration.
- [ ] Implement constraints, indexes and RLS; use RPC for response creation to make duplicate prevention atomic.
- [ ] Add repository tests for page, create/close/respond, retry and permission error.
- [ ] Run `supabase db reset && supabase test db` and `./gradlew :app:testDebugUnitTest`; expected: all pass.
- [ ] Connect opportunities to project details; mark compensation terms as entered by project maintainers and provide no payment flow.

### Task 2: Add blocks and reports with restricted case access

**Files:**
- Create: `backend/supabase/migrations/20261003000310_blocks_reports.sql`
- Create: `backend/supabase/tests/database/blocks_reports.test.sql`
- Create: `backend/supabase/functions/submit-report/index.ts`
- Create: `backend/supabase/functions/moderate-report/index.ts`
- Create: `backend/supabase/tests/functions/moderation.test.ts`

**Interfaces:** Tables `user_blocks(blocker_id, blocked_id, created_at)`, `reports(id, reporter_id, subject_type, subject_id, reason_code, details, state, created_at)`, and append-only `moderation_actions(id, report_id, actor_id, action, reason, created_at)`. Unique block pairs and report access follows the backend spec. Moderation function accepts `{ report_id, action, reason }` only from an active moderator/admin.

- [ ] Write pgTAP checks that a reporter sees their own report status, another member cannot read any report, a moderator sees report details, and the moderator cannot query unrelated private messages.
- [ ] Write function tests for unauthenticated, ordinary member, revoked moderator, invalid action, repeated request idempotency, and successful audited action.
- [ ] Run `supabase test db` and function tests; expected: access tests fail before migration/functions.
- [ ] Implement block uniqueness, report state transitions, minimal report context, moderator-only functions and append-only action rows. Set a safe `search_path` for any `SECURITY DEFINER` function and revoke public execute.
- [ ] Add a common database predicate used by feed, opportunity, messaging (later plan), and notification (later plan) queries to suppress blocked interactions.
- [ ] Run `supabase db reset && supabase test db`; expected: report confidentiality, moderator scope and block tests pass.

### Task 3: Add member safety and moderator Android flows

**Files:**
- Modify: `app/src/main/java/com/tnhc/community/data/CommunityRepository.kt`
- Create: `app/src/main/java/com/tnhc/community/data/ModerationCase.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/ProjectScreens.kt`
- Create: `app/src/main/java/com/tnhc/community/ui/ModerationScreen.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/CommunityApp.kt`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`

**Interfaces:** Repository exposes `blockMember(userId)`, `unblockMember(userId)`, `submitReport(subjectType, subjectId, reasonCode, details)`, `loadMyReports(cursor)`, `loadModerationQueue(cursor)`, and `applyModerationAction(reportId, action, reason)`. Queue methods return forbidden for non-moderators.

- [ ] Add fake-repository UI tests for report submission success/failure, block confirmation/unblock, empty queue, moderator denial and action result.
- [ ] Run `./gradlew :app:testDebugUnitTest`; expected: missing screen behavior fails.
- [ ] Implement member report/block entry points, outcome states, and role-gated moderator queue. Do not show reporter identity to other members or expose full message context.
- [ ] Add content suppression after a block in feed/project discussions and enforce server errors visibly with retry-safe UI.
- [ ] Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`; expected: all pass.

### Task 4: Prove the external-participant safety gate

**Files:**
- Modify: `backend/supabase/tests/database/blocks_reports.test.sql`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`
- Modify: `docs/SECURITY_AND_MODERATION.md`, `docs/TESTING.md`
- Modify: `FEATURES.md`, `features.json`, `features.csv`, `ROADMAP.md`
- Create: `docs/development/0.0.4-trust-moderation.md`

**Interfaces:** Report outcomes reference a documented policy and support/appeal path; the API never returns privileged case fields to ordinary callers.

- [ ] Run cross-account tests for report creation/read, block suppression, moderator action, revoked moderator, and unrelated conversation denial.
- [ ] Rehearse an abuse report from Android submission through moderator review, action, reporter status and appeal/support route using synthetic accounts.
- [ ] Run `supabase db reset && supabase test db` and `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`; expected: all pass.
- [ ] Update release gates and development evidence. Do not allow external participants until the rehearsal and moderator response ownership are documented.
