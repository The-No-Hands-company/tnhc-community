# Feature register

TNHC-001 and TNHC-002 are implemented and in review for alpha 0.0.1; see [validation evidence](docs/development/0.0.1-foundation.md). TNHC-003 through TNHC-005 are in review for alpha 0.0.2; see [development evidence](docs/development/0.0.2-backend-accounts.md). TNHC-006, TNHC-007 and TNHC-017 are in progress for alpha 0.0.3; the shared Community directory/feed and Founder console are implemented, with live cross-client verification still open. See [the Founder build record](docs/development/0.0.3-founder-build.md). No feature is released. Remaining items are planned. Targets are tentative. P1 = core scope; P2 = later expansion. Stable IDs are never reused.

| ID | Feature | Status | Target | Dependencies | Acceptance criteria |
|---|---|---|---|---|---|
| TNHC-001 | Android shell and navigation | in_review | 0.0.1 | None | Installable Android build with five destinations and working back navigation. |
| TNHC-002 | Project catalogue and detail | in_review | 0.0.1 | TNHC-001 | Browse local demo projects; view stage, ownership category, tags and details; empty/error states present. |
| TNHC-003 | Authentication and backend foundation | in_progress | 0.0.2 | TNHC-001 | Invite-only email/password sign-in/out, invitation password setup, encrypted sessions and backend permissions are verified; Google sign-in follows OAuth and account-linking review; public deployment prerequisites remain. |
| TNHC-004 | Member profiles and interests | in_progress | 0.0.2 | TNHC-003 | Members can edit their profile/interests and choose member/private visibility; cross-account restrictions and invitation-session restoration are verified. |
| TNHC-005 | Project following | in_progress | 0.0.2 | TNHC-002, TNHC-003 | Signed-in members can follow/unfollow with persisted state; retry behavior and emulator integration are verified. |
| TNHC-006 | Global feed and rich text interactions | in_progress | 0.0.3 | TNHC-003, TNHC-005 | Topic text posting is implemented; complete and verify feed pagination, comments/replies, reactions, text/code selection and copy; official announcements require maintainer role. |
| TNHC-007 | Topic communities | in_progress | 0.0.3 | TNHC-006 | Browse public topics and recent posts, join/leave, and publish as an active member; verify the same content and Founder-managed changes across both clients. |
| TNHC-008 | Collaboration opportunities | planned | 0.0.4 | TNHC-006 | Maintainers publish requests with skills, terms and open/closed state; members respond. |
| TNHC-009 | Reports blocks verification and moderation | planned | 0.0.4 | TNHC-003, TNHC-006 | Members report/block; authorized moderators review and record actions; verification badges are granted through an audited process; reporter access restricted. |
| TNHC-010 | Private one-to-one messaging | planned | 0.0.5 | TNHC-003, TNHC-009 | Private real-time text messages support quoted replies, read receipts and typing indicators; only participants access messages; retries do not duplicate sends; blocking prevents new contact. |
| TNHC-011 | Notifications and preferences | planned | 0.0.5 | TNHC-005, TNHC-010 | Members control notifications; taps open authorised content; blocked contact cannot notify. |
| TNHC-012 | Search and discovery | planned | 0.0.5 | TNHC-002, TNHC-007 | Search projects/topics by agreed fields with paginated results and empty states. |
| TNHC-013 | Recovery deletion and operations | planned | 0.1.0 | TNHC-003, TNHC-009 | Recovery/deletion flows verified; support route, retention policy and tested backup restoration exist. |
| TNHC-014 | Accessibility and reliability refinement | planned | 0.2.0 | TNHC-010, TNHC-012, TNHC-013 | Text scaling, accessibility navigation, interrupted requests and agreed performance targets verified. |
| TNHC-015 | Member project publishing pilot | planned | 1.1.0 | TNHC-009, TNHC-014 | Approved members publish projects; ownership and official/community distinction enforced. |
| TNHC-016 | Marketplace, job and service listings | planned | 1.2.0 | TNHC-015 | Listings cover project opportunities, jobs, gigs and member services with accountable seller, description and enquiry route; report handling exists; checkout excluded. |
| TNHC-017 | Founder build and community controls | in_progress | 0.0.3 | TNHC-003, TNHC-006, TNHC-007 | Separate Founder APK; sole server-provisioned Founder role; audited member, project, topic, post/comment moderation and app-setting controls; no embedded authority or general private-message access. |
| TNHC-018 | Organization profiles and teams | planned | 0.2.0 | TNHC-003, TNHC-004 | Organizations have distinct pages for services, team members and updates; members control affiliation; permissions distinguish individual and organization actions. |
| TNHC-019 | Offline message cache | planned | 0.0.5 | TNHC-010 | Recent authorized conversations remain readable from a local database offline; writes clearly show queued versus server-accepted state. |
| TNHC-020 | Media posts and push notifications | planned | 0.1.0 | TNHC-006, TNHC-011 | Members upload supported post images and receive preference-controlled push notifications that open content they may access. |
| TNHC-021 | Member and organization discovery | planned | 0.2.0 | TNHC-012, TNHC-018 | Search people and organizations by agreed industry, skills, tech stack, location and role filters with pagination and privacy-aware visibility. |
| TNHC-022 | Web community administration dashboard | planned | 0.1.0 | TNHC-009, TNHC-017 | Authorized staff review reports, moderate content, handle bans and manage community settings through audited server-checked controls. |
| TNHC-023 | Group direct messaging | planned | Unscheduled | TNHC-010, TNHC-009 | Group membership, message visibility, blocking, reporting and notification behavior are defined and tested before release. |
| TNHC-024 | 24-hour status stories | planned | Unscheduled | TNHC-006, TNHC-009, TNHC-020 | Short-lived updates expire reliably and retain reporting, audience and moderation controls. |
| TNHC-025 | Voice and video calling | planned | Unscheduled | TNHC-010, TNHC-009 | Calls use an approved real-time transport with consent, blocking, abuse reporting and reliable connection-state handling. |

Owners, evidence and actual introduced versions are empty until assigned/verified/released; maintain them in the tracking exports.
