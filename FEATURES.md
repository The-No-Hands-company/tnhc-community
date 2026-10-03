# Feature register

TNHC-001 and TNHC-002 are implemented and in review for alpha 0.0.1; see [validation evidence](docs/development/0.0.1-foundation.md). No feature is released. Other items remain planned. Targets are tentative. P1 = core scope; P2 = later expansion. Stable IDs are never reused.

| ID | Feature | Status | Target | Dependencies | Acceptance criteria |
|---|---|---|---|---|---|
| TNHC-001 | Android shell and navigation | in_review | 0.0.1 | None | Installable Android build with five destinations and working back navigation. |
| TNHC-002 | Project catalogue and detail | in_review | 0.0.1 | TNHC-001 | Browse local demo projects; view stage, ownership category, tags and details; empty/error states present. |
| TNHC-003 | Authentication and backend foundation | planned | 0.0.2 | TNHC-001 | Sign in/out; protected operations enforce server permissions; isolated test accounts. |
| TNHC-004 | Member profiles and interests | planned | 0.0.2 | TNHC-003 | Edit own profile and interests; another member cannot edit it. |
| TNHC-005 | Project following | planned | 0.0.2 | TNHC-002, TNHC-003 | Follow/unfollow persists and duplicate follows are prevented. |
| TNHC-006 | Updates and comments | planned | 0.0.3 | TNHC-003, TNHC-005 | Publish permitted posts/comments; official announcements require maintainer role; lists paginate. |
| TNHC-007 | Topic communities | planned | 0.0.3 | TNHC-006 | Discover shared topics and participate with visible author attribution. |
| TNHC-008 | Collaboration opportunities | planned | 0.0.4 | TNHC-006 | Maintainers publish requests with skills, terms and open/closed state; members respond. |
| TNHC-009 | Reports blocks and moderation | planned | 0.0.4 | TNHC-003, TNHC-006 | Members report/block; authorised moderator reviews and records action; reporter access restricted. |
| TNHC-010 | Private messaging | planned | 0.0.5 | TNHC-003, TNHC-009 | Only participants access messages; retries do not duplicate sends; blocking prevents new contact. |
| TNHC-011 | Notifications and preferences | planned | 0.0.5 | TNHC-005, TNHC-010 | Members control notifications; taps open authorised content; blocked contact cannot notify. |
| TNHC-012 | Search and discovery | planned | 0.0.5 | TNHC-002, TNHC-007 | Search projects/topics by agreed fields with paginated results and empty states. |
| TNHC-013 | Recovery deletion and operations | planned | 0.1.0 | TNHC-003, TNHC-009 | Recovery/deletion flows verified; support route, retention policy and tested backup restoration exist. |
| TNHC-014 | Accessibility and reliability refinement | planned | 0.2.0 | TNHC-010, TNHC-012, TNHC-013 | Text scaling, accessibility navigation, interrupted requests and agreed performance targets verified. |
| TNHC-015 | Member project publishing pilot | planned | 1.1.0 | TNHC-009, TNHC-014 | Approved members publish projects; ownership and official/community distinction enforced. |
| TNHC-016 | Marketplace listings and enquiries | planned | 1.2.0 | TNHC-015 | Listings have seller, description and enquiry route; report handling exists; checkout excluded. |

Owners, evidence and actual introduced versions are empty until assigned/verified/released; maintain them in the tracking exports.
