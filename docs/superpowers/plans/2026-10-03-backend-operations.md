# Backend Operations, Recovery, and Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the closed pilot recoverable and supportable through tested backups/restores, account recovery/deletion, secure deployment, monitoring and operational ownership.

**Architecture:** Self-host the pinned Supabase Docker Compose release on the existing machine only after capacity, network, HTTPS, email and backup prerequisites are verified. Keep administration private; publish only HTTPS gateway endpoints. Use encrypted off-machine database and media backups, and rehearse restoration before beta.

**Tech Stack:** Supabase self-hosted Docker Compose, PostgreSQL backup/restore tools, Linux firewall/reverse proxy/TLS, SMTP provider if available, Kotlin/Compose, runbooks and shell scripts.

**Spec:** [Backend architecture plan](../../BACKEND_ARCHITECTURE.md), network/operations, invite workflow, privacy, and 0.1.0 gates.

## Global Constraints

- Target beta 0.1.0; product version stays 0.0.0 until the release gate passes.
- The local stack and all earlier backend plans pass first.
- No new paid hostname, hardware, email, backup or monitoring service without separate owner approval.
- Secrets are generated outside Git, restricted to the operator, and rotated on exposure.
- Public traffic uses HTTPS; Postgres, Docker management and Studio are never public.
- A same-disk copy is not an adequate backup; restore both database and media from a separate destination.
- Data retention, authored-content deletion behavior, report/message retention and support ownership are settled before pilot access.

---

### Task 1: Verify hardware and network prerequisites without exposing data

**Files:**
- Create: `docs/operations/backend-host-assessment.md`
- Modify: `docs/BACKEND_ARCHITECTURE.md`

**Interfaces:** Assessment records OS, Docker support, RAM, SSD free space, backup device, uptime, public hostname/DNS ownership, inbound HTTPS reachability, TLS renewal path, SMTP test result and an explicit monthly cost estimate of $0 or a listed exception.

- [ ] Run read-only checks on the intended host: `uname -a`, `docker --version`, `docker compose version`, `free -h`, `df -h`, and inspect router/ISP inbound access.
- [ ] Compare with documented minimum (4 GB RAM, 40 GB SSD) and recommended (8 GB RAM, 80 GB SSD); expected: a dated pass/fail record with actual values, not estimates.
- [ ] Confirm a hostname resolves to the host and HTTPS certificate can be renewed. Do not open the firewall until the reverse proxy is configured and reviewed.
- [ ] Test invitation/recovery email delivery using a non-production mailbox. If no reliable SMTP path or hostname exists, mark public deployment blocked and request a cost decision rather than buying a service.
- [ ] Identify separate backup storage and test that it is writable, encrypted and not the server's primary disk.

### Task 2: Define retention, account recovery, and deletion behavior

**Files:**
- Create: `docs/operations/data-retention.md`
- Create: `backend/supabase/migrations/20261003000500_account_lifecycle.sql`
- Create: `backend/supabase/tests/database/account_lifecycle.test.sql`
- Create: `backend/supabase/functions/delete-account/index.ts`
- Create: `backend/supabase/functions/request-account-recovery/index.ts`

**Interfaces:** Lifecycle functions require the account owner to reauthenticate for deletion. Recovery uses Auth's email reset path. `delete-account` returns `{ request_id, state }`; deletion jobs erase credentials/profile/device tokens/media and apply the approved policy to authored posts/messages/reports. The retention policy document defines each entity outcome and deadline before the migration is written.

- [ ] Draft the entity-by-entity policy for profile, authored public posts/comments, private messages, project records, reports, moderation audit, media, notifications, push tokens and backup expiry.
- [ ] Obtain owner review of that policy before implementing destructive deletion behavior; no deletion migration runs on pilot data before approval.
- [ ] Write tests for unauthorized delete, reauthentication, in-progress/retry idempotency, foreign object ownership, retained audit requirements and expired recovery link.
- [ ] Implement lifecycle functions and scheduled cleanup only after the policy is approved; log request IDs/state without logging content.
- [ ] Run `supabase db reset && supabase test db` plus function tests; expected: all lifecycle cases pass on synthetic data.

### Task 3: Add secure host deployment and operational runbook

**Files:**
- Create: `backend/deploy/compose.production.yml`
- Create: `backend/deploy/Caddyfile`
- Create: `backend/deploy/check-health.sh`
- Create: `docs/operations/self-hosted-runbook.md`
- Modify: `backend/README.md`

**Interfaces:** The deployment uses a pinned upstream self-hosted release, required generated environment secrets, private administration binding, persistent volumes and HTTPS gateway. Health script checks container health, HTTPS endpoint, disk space and last successful backup without printing secrets.

- [ ] Pin the Supabase self-hosted release and image versions; record upgrade/rollback commands from the upstream release notes.
- [ ] Configure the reverse proxy for TLS and gateway traffic; bind Studio to localhost/private network and configure host firewall to reject database/Docker ports from WAN.
- [ ] Generate `.env` outside the repository with restrictive file permissions; validate no placeholder secrets remain; do not run any upstream quickstart script without inspecting it.
- [ ] Start a synthetic-data deployment on a private/test host and verify public scan shows only required HTTPS service; expected: admin/database ports are unreachable from outside.
- [ ] Write outage, upgrade, secret rotation, account abuse, failed backup and rollback runbooks with operator/owner contact route.
- [ ] Run the health script and runbook steps; expected: services healthy, TLS valid, admin private, no secrets printed.

### Task 4: Implement encrypted backup, restore, and monitoring checks

**Files:**
- Create: `backend/ops/backup.sh`
- Create: `backend/ops/restore.sh`
- Create: `backend/ops/verify-backup.sh`
- Create: `backend/ops/backup-metrics.sh`
- Modify: `docs/operations/self-hosted-runbook.md`

**Interfaces:** `backup.sh` writes timestamped encrypted Postgres and Storage archives to a configured separate destination, applies retention and returns nonzero on partial failure. `verify-backup.sh` checks archive integrity and age. `restore.sh` requires an explicit confirmation token and targets an isolated restore environment, never overwriting production by default.

- [ ] Write shell tests against temporary directories for successful archive, encryption failure, missing media, destination unavailable, checksum mismatch, retention and restore confirmation refusal.
- [ ] Implement dumps and media copy with checksums, encryption using an operator-held key, restrictive permissions, retention from `data-retention.md`, and nonzero exit on any component failure.
- [ ] Schedule backup only after manual verification; collect last-success timestamp, backup size, disk capacity, unhealthy containers, and expired certificate checks.
- [ ] Restore the latest encrypted backup into a clean isolated Supabase deployment and compare row counts, key constraints, media object counts and sampled checksums.
- [ ] Record recovery point and recovery time achieved; expected: within owner-approved objectives. If not, do not open the pilot.

### Task 5: Pass the beta operations gate

**Files:**
- Modify: `docs/RELEASE_CHECKLIST.md`, `docs/TESTING.md`, `docs/SECURITY_AND_MODERATION.md`
- Modify: `FEATURES.md`, `features.json`, `features.csv`, `ROADMAP.md`
- Create: `docs/development/0.1.0-backend-operations.md`

**Interfaces:** The release record links the host assessment, retention policy, TLS check, backup/restore evidence, support route, incident/rollback rehearsal and data-access tests.

- [ ] Rehearse a full restore, account recovery, account deletion, backup failure alert, service rollback and report-to-response path using synthetic pilot data.
- [ ] Verify current data privacy and retention behavior, registration remains invite-only, and all service credentials are outside source/APK.
- [ ] Run `supabase db reset && supabase test db`, backend function and ops script tests, plus `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`; expected: all pass.
- [ ] If any paid prerequisite is needed, stop before incurring it and present exact options to the owner. Otherwise, request a separate owner decision to expose the closed pilot.
- [ ] Update feature tracking and release evidence only after review; a passing technical checklist does not itself open registration or authorize spending.
