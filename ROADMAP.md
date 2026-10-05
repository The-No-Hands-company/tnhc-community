# Roadmap

Versions and milestones below are not releases. The 0.0.1 foundation is implemented and undergoing verification; see [its development record](docs/development/0.0.1-foundation.md). Alpha 0.0.2 backend/accounts work is in review; see [its development record](docs/development/0.0.2-backend-accounts.md). No deadlines are implied. Targets can change through a documented decision. Feature status is authoritative in FEATURES.md.

| Version | Stage and scope | Completion gate |
|---|---|---|
| 0.0.0 | Planning baseline, tracking, platform and product scope | Documentation and unresolved decisions recorded |
| 0.0.1 | Alpha foundation: Android shell, five destinations, local project catalogue and detail | Installable Android build; clearly labelled demo data; basic navigation verified |
| 0.0.2 | Alpha identity: invite-only email/password accounts, individual profiles and project follows; add Google sign-in after provider and account-linking review | Two test accounts remain isolated; sign-in, profile access and follows pass backend and Android checks |
| 0.0.3 | Alpha participation: global/community text feed, project updates, comments/replies, reactions, topic discussions and separate Founder console | Member/maintainer permissions, Founder authorization/audit controls, pagination, text selection/copy, empty/error states verified |
| 0.0.4 | Alpha trust: collaboration requests, report/block, moderator queue and verified-member/organization indicators | Reporting, blocking, verification grants and moderator actions are auditable and work end to end before outside community access |
| 0.0.5 | Alpha communication: private 1:1 real-time text messages, quoted replies, read/typing indicators and local message caching | Message privacy, blocked-contact behavior, retry safety and offline cache behavior verified |
| 0.1.0 | First beta: recovery/deletion, push notifications, image uploads, web-based admin dashboard and closed community pilot | Security and moderation gates pass; no critical defects; recoverable backend; pilot onboarding and operational control work |
| 0.1.1 | Beta corrections from pilot | Fixes verified; regressions checked; release notes published |
| 0.2.0 | Beta expansion: individual and organization profiles, member/company directory filters (industry, skills, tech stack and location), accessibility, slower-network and larger-catalogue improvements | Profile privacy and organization membership rules pass; agreed performance/accessibility checks pass on representative Android devices |
| 1.0.0 | First stable public community release | Stable core, operating moderation, support route, accurate privacy disclosures, release and rollback checks complete |
| 1.1.0 | Tentative member project publishing pilot | Ownership, approval and official/community labels verified |
| 1.2.0 | Tentative marketplace discovery and enquiries | Listing rules, seller identity, abuse handling verified; no implied in-app checkout |
| Unscheduled | 24-hour stories, group DMs, voice/video calling, transactions, events, richer matching, contributor-owned iOS | Separate privacy, moderation, reliability, cost and resourcing review before scheduling |

## Core beta journey

Create account → select interests → discover TNHC project → follow → discuss/update → find an opportunity → contact a collaborator → return through a relevant notification.

## Delivery phases

The MVP follows four capability phases across the version milestones above:

1. **Foundation and feed (0.0.2–0.0.4):** invite-only email/password and reviewed Google sign-in; individual profiles; a global/community text feed with replies, reactions and copyable text; reporting and verification before broader access.
2. **Connection (0.0.5):** private 1:1 text messaging and local caching first. Group messaging, media sharing and calling wait for separate privacy and reliability checks.
3. **Expansion (0.1.0–0.2.0):** push notifications, image uploads, searchable member and organization profiles, and structured discovery filters.
4. **Control (0.0.3–0.1.0):** the private Founder Android console is being built now; a web admin dashboard follows before the closed beta so moderation and operations have a dedicated control surface.

Marketplace and service listings follow the core member journey. Stories and voice/video calls remain unscheduled until community usage, moderation capacity, privacy requirements and operating costs justify them.

## Scope discipline

Start with selected active TNHC projects and a searchable catalogue that can grow beyond 300 entries. Avoid creating inactive discussion spaces for every project. Catalogue migration requires a separately verified source inventory; this pack does not assert those projects are already imported.

Payments, subscriptions, video calls, crowdfunding, automated matching, and iOS are outside the initial release. Marketplace listing and payment processing are separate features.
