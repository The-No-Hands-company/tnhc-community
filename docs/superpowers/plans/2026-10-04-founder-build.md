# Founder Build and Control Plane Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the TNHC founder a separate Android build for direct, audited control of in-app community records and application settings, with every privileged decision enforced by the backend.

**Architecture:** Add a server-provisioned, single-holder `founder` platform role and explicit Founder-authorized database operations. Add a distinct Android product flavor that exposes Founder screens only when the authenticated server session confirms that role; the flavor itself grants no privilege. Expand the Founder console as project, content, trust, messaging, and application controls are implemented.

**Tech Stack:** Supabase/PostgreSQL migrations and pgTAP, Supabase Edge Functions and Deno tests, Kotlin, Gradle product flavors, Jetpack Compose, existing `supabase-kt` repository.

**Spec:** [Founder build and control plane design](../specs/2026-10-04-founder-build-design.md)

## Global Constraints

- Current product version stays 0.0.0; the work is an unreleased alpha increment.
- Founder is a backend role, provisioned or recovered only by a server/database operator.
- There is one Founder at a time; regular administrators cannot grant, remove, or impersonate this role.
- No secret, service-role key, signing key, or Founder authority is embedded in either APK.
- Founder mutations and application-setting changes are atomic with append-only, redacted audit records.
- Both product flavors use the same API contract; RLS and narrow functions remain the authorization boundary.
- Private communications remain report-scoped under the privacy policy; Founder does not receive general message-body access.
- The Founder app controls in-app records only; host, backup, DNS, certificate, and deployment operations stay in the operator runbook.
- Keep public deployment gated on verified HTTPS, SMTP, off-machine backups, recovery, and support.

---

### Task 1: Add the server-owned Founder identity and role boundary

**Files:**
- Create: `backend/supabase/migrations/20261003000150_founder_identity.sql`
- Create: `backend/supabase/tests/database/founder_identity.test.sql`
- Modify: `backend/README.md`
- Modify: `docs/API_CONTRACT.md`

**Interfaces:** Add `founder` to `platform_roles.role`; enforce at most one row with that role. Add `public.viewer_is_founder() returns boolean`, which checks only `auth.uid()`. The function is safe to call by an authenticated client and exposes no other user's role. Database operator provisions the role directly through a documented local/production SQL procedure. Authenticated callers cannot insert, update, or delete `founder` role assignments.

- [x] Write pgTAP tests proving the founder role constraint, single-holder index, own-role predicate, anonymous denial, and inability of members/admins to assign or remove Founder.
- [x] Run `npx --yes supabase@2.119.0 test db --workdir backend`; new tests fail because the Founder role and predicate do not exist.
- [x] Add a forward migration that replaces the existing role check constraint, adds the partial unique index and caller-only helper, and documents operator provisioning without a UUID or secret in source.
- [x] Add Founder read policies for member profiles, platform roles, projects, and project memberships; preserve existing member policies and column grants.
- [x] Run `npx --yes supabase@2.119.0 db reset --workdir backend && npx --yes supabase@2.119.0 test db --workdir backend`; Founder and existing core assertions pass.
- [x] Confirm `service_role` remains the only API role able to manage Founder assignments and regular admin RPCs cannot change the Founder row.

### Task 2: Add audited Founder mutations and application settings

**Files:**
- Create: `backend/supabase/migrations/20261003000160_founder_controls.sql`
- Create: `backend/supabase/tests/database/founder_controls.test.sql`
- Modify: `backend/supabase/tests/database/core_schema.test.sql`
- Modify: `docs/DATA_MODEL.md`
- Modify: `docs/API_CONTRACT.md`

**Interfaces:** Create append-only `admin_audit_log(id, actor_id, action, target_type, target_id, summary, created_at)` and `app_settings(key, value, updated_by, updated_at)`. Expose narrow functions `founder_set_member_state(p_user_id uuid, p_state text)`, `founder_set_platform_role(p_user_id uuid, p_role text, p_enabled boolean)`, and `founder_set_app_setting(p_key text, p_value jsonb)`. Each function verifies Founder, validates an allow-listed operation, performs one mutation, and writes its audit entry in the same transaction. Only the Founder can read the audit log; no caller can update or delete it. Initial settings allow `maintenance_notice` and `feature_flags` only.

- [x] Write database tests for non-Founder denial, Founder success, valid and invalid member states, ordinary role changes, immutable Founder assignment, allow-listed settings, audit atomicity, and denied audit update/delete.
- [x] Run database tests; verify the new cases fail before the controls migration.
- [x] Implement the audit/settings tables, least-privilege grants, role checks, and narrow functions. Never include credential, invitation-link, report-evidence, or private-message content in summaries.
- [x] Run a clean database reset and all database tests; verify a failed mutation creates no audit row and each successful mutation creates exactly one.
- [x] Document function request/response behavior, stable errors, and audit fields in the API contract.

### Task 3: Add the separate Founder Android product flavor

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/tnhc/community/MainActivity.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/CommunityApp.kt`
- Create: `app/src/main/java/com/tnhc/community/ui/FounderAccess.kt`
- Create: `app/src/test/java/com/tnhc/community/ui/FounderAccessTest.kt`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`

**Interfaces:** Gradle flavors are `member` and `founder` in one flavor dimension. `BuildConfig.FOUNDER_BUILD` controls only screen/navigation inclusion. The current member application ID and launcher name remain unchanged; the Founder flavor uses a distinct `.founder` application ID and “TNHC Founder” label. `FounderAccess` represents `Loading`, `Denied`, and `Allowed`; only a role result read from the authenticated backend can produce `Allowed`.

- [x] Write failing unit and Compose tests: the member build has no Founder navigation; Founder build plus Founder role exposes the control entry; Founder build plus regular account, denied request, or role-load failure exposes no controls.
- [x] Run `./gradlew :app:testDebugUnitTest :app:assembleMemberDebug :app:assembleFounderDebug :app:assembleDebugAndroidTest`; verify the new tests fail because the flavor and role gate are absent.
- [x] Add the flavors, stable member ID, distinct Founder identity, role-gated route, and explicit loading/denied states. Keep user-facing operations behind repository interfaces.
- [x] Run the same Gradle tasks and confirm both APKs compile with distinct IDs and no elevated credentials.

### Task 4: Implement core Founder community controls

**Files:**
- Create: `app/src/founder/java/com/tnhc/community/data/FounderRepository.kt`
- Create: `app/src/founder/java/com/tnhc/community/ui/FounderConsole.kt`
- Create: `app/src/testFounder/java/com/tnhc/community/data/FounderRepositoryTest.kt`
- Create: `app/src/androidTestFounder/java/com/tnhc/community/FounderConsoleTest.kt`
- Modify: `app/src/androidTest/java/com/tnhc/community/CommunityAppTest.kt`
- Modify: `backend/supabase/functions/invite-member/index.ts`
- Modify: `backend/supabase/tests/functions/invite-member.test.ts`
- Modify: database migrations/tests for project and topic management as each operation is added.

**Interfaces:** `FounderRepository` exposes `loadOverview()`, `setMemberState(userId, state)`, `setPlatformRole(userId, role, enabled)`, `setProjectMembership(projectId, userId, role)`, and `setAppSetting(key, value)`. `loadOverview()` returns bounded member/project counts plus paginated manageable records, never service credentials. `invite-member` permits an administrator or Founder and writes a redacted audit event after successful invitation creation.

- [x] Add fake-transport tests for overview pagination, invite error handling, member suspension/reactivation, role assignment, settings validation, retry behavior, and audit-safe responses.
- [x] Add API tests proving regular members and ordinary moderators receive 403 for Founder mutations, and administrators cannot modify the sole Founder assignment.
- [x] Add authenticated invite authorization for the Founder role and tests for accepted/denied roles while preserving the existing invitation flow.
- [x] Implement member state, platform role, project membership, project and topic maintenance, and app setting operations one at a time as audited server functions.
- [x] Connect Compose screens with search, bounded lists, confirmation for suspension/removal, save/error/retry states, and visible audit history. Require server readback after each mutation.
- [x] Run unit, Deno, and pgTAP tests after each vertical slice.
- [ ] Run two-account live-backend Android tests proving the regular account cannot see or call Founder controls; this is distinct from the passing fake-session UI tests.

### Task 5: Integrate Founder controls with content and moderation

**Files:**
- Modify: `backend/supabase/migrations/20261003000200_community_content.sql`
- Modify: `backend/supabase/tests/database/community_content.test.sql`
- Create: `backend/supabase/migrations/20261003000230_founder_content_controls.sql`
- Create: `backend/supabase/tests/database/founder_content_controls.test.sql`
- Modify: `app/src/main/java/com/tnhc/community/data/FounderRepository.kt`
- Modify: `app/src/main/java/com/tnhc/community/ui/FounderConsole.kt`
- Modify: `docs/superpowers/plans/2026-10-03-backend-community-content.md`

**Interfaces:** Founder content operations are `founder_set_content_moderation(p_content_type text, p_content_id uuid, p_state text)` and bounded queries for hidden/visible posts and comments. They may change moderation state but cannot impersonate the author. Every change writes an audit row. The `founder` role is included in the database from the first content migration's final version before that migration is released.

- [x] Repair and run the existing content migration tests first, preserving active-member, topic-membership, maintainer-only official update, and author-only edit/delete rules.
- [x] Add failing pgTAP tests proving Founder can review and hide/restore any post/comment, ordinary authors cannot change moderation state, members cannot read hidden content, and each Founder moderation change is audited.
- [x] Add bounded Founder review queries and mutation function. Keep comment/body content out of the audit summary.
- [x] Add Founder UI moderation queue, detail access under existing privacy rules, hide/restore/remove confirmation, and audit readback.
- [x] Run full clean-reset pgTAP, Deno, unit, lint, debug APK, and Android instrumentation gates.

### Task 6: Complete Founder release evidence and tracking

**Files:**
- Modify: `FEATURES.md`, `features.json`, `features.csv`, `ROADMAP.md`
- Create: `docs/development/0.0.3-founder-build.md`
- Modify: `backend/README.md`, `docs/BACKEND_ARCHITECTURE.md`, `docs/BUILDING.md`

**Interfaces:** Document exact install/build commands for member and Founder variants, the out-of-band provisioning/recovery procedure, supported Founder operations, and the audit trail. Never document or store a production Founder UUID, password, key, or invitation URL in source control.

- [ ] Add two-account live-backend tests for Founder role loading, member denial, successful audited management, app setting readback, and session restart.
- [x] Run `npx --yes supabase@2.119.0 db reset --workdir backend && npx --yes supabase@2.119.0 test db --workdir backend`, all Deno tests, and `./gradlew :app:testDebugUnitTest :app:assembleMemberDebug :app:assembleFounderDebug :app:lintMemberDebug :app:lintFounderDebug :app:assembleDebugAndroidTest`.
- [x] Scan both APKs for privileged credential markers and verify the regular build contains no Founder navigation.
- [x] Record executed results and synchronize feature exports. Keep actual introduced versions blank until release; keep public deployment pending its separate operations gate.
- [x] Rebuild after the horizontal-scroll navigation adjustment and run shared Founder/member instrumentation on the API 36 emulator. Gradle must use JDK 21 here; local socket access is needed for its lock service.
