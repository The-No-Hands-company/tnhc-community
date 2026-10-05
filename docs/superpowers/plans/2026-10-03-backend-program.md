# Real Backend Program Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a secure backend for TNHC Community in independently verifiable alpha and beta increments.

**Architecture:** Self-host Supabase on existing hardware after the capacity, network, email, and backup prerequisites pass. Use PostgreSQL migrations and row-level security as the source of truth; expose the same Auth, REST, Storage, Realtime, and function APIs to Android and a future iOS client.

**Tech Stack:** Supabase self-hosted Docker Compose, PostgreSQL, SQL migrations and pgTAP, Supabase Auth/PostgREST/Storage/Realtime/Edge Functions, Kotlin/Jetpack Compose Android, Firebase Cloud Messaging for Android push, Gradle CI.

**Spec:** [Backend architecture plan](../../BACKEND_ARCHITECTURE.md) and [ADR-0011](../../decisions/ADR-0011-self-hosted-backend.md).

Status: approved for inline execution; backend implementation is paused while the Android SDK environment is restored.

## Global Constraints

- Current product version stays 0.0.0 until a release passes its documented gate.
- Alpha targets remain 0.0.2 accounts/follows, 0.0.3 content, 0.0.4 trust, and 0.0.5 messaging/discovery.
- The initial pilot is invite-only; open self-registration stays disabled.
- The existing always-on machine is the zero-additional-monthly-hosting target; no paid service, hostname, email plan, or backup service may be purchased without a separate decision.
- Supabase CLI commands run from `backend/`; Android Gradle commands run from the repository root.
- Minimum Android SDK remains 26; backend credentials and service-role keys never enter the APK or Git.
- All private data access is enforced server-side; PostgreSQL row-level security denies by default.
- Do not expose Postgres, Docker, or Supabase Studio to public internet; public endpoints require valid HTTPS.
- Each feature remains planned until implementation and its acceptance evidence are verified; keep `FEATURES.md`, `features.json`, and `features.csv` synchronized.

---

## Plan set and dependency order

This program is split so each stage leaves a usable, testable backend increment:

1. [Accounts, projects, and follows](2026-10-03-backend-accounts-follows.md) — TNHC-003–005 / 0.0.2.
2. [Community content and media](2026-10-03-backend-community-content.md) — TNHC-006–007 / 0.0.3.
3. [Opportunities and trust](2026-10-03-backend-trust-moderation.md) — TNHC-008–009 / 0.0.4.
4. [Messaging, notifications, and search](2026-10-03-backend-messaging-discovery.md) — TNHC-010–012 / 0.0.5.
5. [Operations, recovery, and deletion](2026-10-03-backend-operations.md) — TNHC-013 / 0.1.0.

Implement in this order. A later plan may be developed locally while an earlier increment is in review, but do not put pilot data on an unverified host. The operational plan can stop before deployment if a prerequisite needs spending or external account changes.

## Program-level completion

- Each plan's SQL authorization tests, feature tests, Android checks, and documentation gates pass.
- A fresh database builds from migrations and seed data; no undocumented dashboard-only schema changes exist.
- Two-account and unauthenticated tests prove isolation across profiles, projects, reports, messages, storage, and search.
- API contracts remain client-independent and contain no privileged keys.
- The 0.1.0 gate includes account recovery/deletion rules, a successful database-and-media restore from a separate destination, support ownership, and a closed-pilot review.
- If any requirement depends on a paid service, pause that external step and bring its exact cost/options to the owner before proceeding.
