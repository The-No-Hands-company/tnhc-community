# ADR-0011 — Self-hosted Supabase backend

| Field | Value |
| --- | --- |
| Date | 2026-10-03 |
| Status | Accepted design; deployment not implemented |
| Owner | TNHC |
| Affected features / versions | TNHC-003–TNHC-013; 0.0.2–0.1.0 |
| Supersedes | None |

## Context

TNHC Community needs shared persistent backend services for identity, profiles, projects, follows, community content, collaboration, moderation, private messaging, notifications, media and search. Android is the first client; a future community-built iOS client should use the same service contract. The owner set a zero additional monthly hosting budget and confirmed an always-on machine and public inbound internet access are available. The app currently has no backend implementation.

Supabase's managed free plan is $0 but can pause projects after one week without activity and does not include automatic backups, making it unsuitable as the assumed always-on pilot production service. See [current pricing](https://supabase.com/pricing). Self-hosting removes a provider subscription but moves security, maintenance, backups, uptime and recovery duties to TNHC.

## Options

1. **Self-host Supabase.** PostgreSQL, Auth, REST, Storage, Realtime and Functions in one Docker deployment. Lowest incremental hosting bill on owned equipment and covers the required backend capabilities. TNHC operates the full stack and must provide separate backups and public HTTPS.
2. **Supabase managed free tier.** Low operational burden and quick development start, but inactivity pausing and lack of automatic backups conflict with a continuous community service. Suitable for isolated development experiments only.
3. **Custom API and database.** Maximum control over product-specific contracts, but adds substantially more implementation, security and operations work without a demonstrated need at this stage.

## Decision

Use self-hosted Supabase as the planned backend platform, deployed with the official Docker Compose self-hosted setup on the existing machine after capacity and network checks. Use PostgreSQL migrations, row-level security, PostgREST, Auth, Storage, Realtime and narrowly scoped Edge Functions. Keep development local and isolated from any closed-pilot data. Invite-only membership is the initial pilot policy; open registration requires a later product/security review.

## Consequences

- Android and future iOS use the same HTTPS Auth/REST/Storage/Realtime/Function APIs. No provider secret or database credential enters a mobile package.
- PostgreSQL row-level security and automated authorization tests are required for every exposed table/object. High-impact workflows are server-controlled and audited.
- Public pilot depends on verified machine capacity, TLS hostname, email delivery for invites/recovery, and encrypted off-machine backups with a successful restore drill.
- Zero monthly service fees are a target, not a guarantee: power, bandwidth, a hostname, SMTP or a separate backup destination may require spending. No purchase is authorized by this ADR.
- Home-hosting has availability and disaster risks. If the host cannot meet agreed recovery/availability needs, revisit managed hosting with a separately approved budget.
- Review this decision if user scale, uptime, data residency, support obligations, operating burden or costs change materially.

## References

- [Backend architecture plan](../BACKEND_ARCHITECTURE.md)
- [Supabase self-hosting overview](https://supabase.com/docs/guides/self-hosting)
- [Supabase Docker deployment and system requirements](https://supabase.com/docs/guides/self-hosting/docker)
- [Supabase pricing](https://supabase.com/pricing)
