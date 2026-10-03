# Roadmap

All versions below are **planned**, not delivered. The 0.0.1 foundation is implemented and undergoing verification; see [its development record](docs/development/0.0.1-foundation.md). No deadlines or implementation claims are implied. Targets can change through a documented decision. Feature status is authoritative in FEATURES.md.

| Version | Stage and scope | Completion gate |
|---|---|---|
| 0.0.0 | Planning baseline, tracking, platform and product scope | Documentation and unresolved decisions recorded |
| 0.0.1 | Alpha foundation: Android shell, five destinations, local project catalogue and detail | Installable Android build; clearly labelled demo data; basic navigation verified |
| 0.0.2 | Alpha accounts: backend foundation, sign-in, profiles, project follow | Two test accounts remain isolated; access tests pass; follows persist |
| 0.0.3 | Alpha participation: project updates, comments, topic discussions | Member/maintainer permissions, pagination, empty/error states verified |
| 0.0.4 | Alpha trust: collaboration requests, report/block, moderator queue | Reporting, blocking and moderator actions work end to end before outside community access |
| 0.0.5 | Alpha communication: direct messages, notifications and search | Message privacy, blocked-contact behavior, notification preferences verified |
| 0.1.0 | First beta: complete core journey, recovery/deletion flows, closed community pilot | Security and moderation gates pass; no critical defects; recoverable backend; pilot onboarding works |
| 0.1.1 | Beta corrections from pilot | Fixes verified; regressions checked; release notes published |
| 0.2.0 | Beta refinement: discovery, accessibility, slower-network and larger-catalogue improvements | Agreed performance/accessibility checks pass on representative Android devices |
| 1.0.0 | First stable public community release | Stable core, operating moderation, support route, accurate privacy disclosures, release and rollback checks complete |
| 1.1.0 | Tentative member project publishing pilot | Ownership, approval and official/community labels verified |
| 1.2.0 | Tentative marketplace discovery and enquiries | Listing rules, seller identity, abuse handling verified; no implied in-app checkout |
| Unscheduled | Transactions, events, richer matching, contributor-owned iOS | Separate requirements and resourcing decision before scheduling |

## Core beta journey

Create account → select interests → discover TNHC project → follow → discuss/update → find an opportunity → contact a collaborator → return through a relevant notification.

## Scope discipline

Start with selected active TNHC projects and a searchable catalogue that can grow beyond 300 entries. Avoid creating inactive discussion spaces for every project. Catalogue migration requires a separately verified source inventory; this pack does not assert those projects are already imported.

Payments, subscriptions, video calls, crowdfunding, automated matching, and iOS are outside the initial release. Marketplace listing and payment processing are separate features.
