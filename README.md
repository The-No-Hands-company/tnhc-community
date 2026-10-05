# TNHC Community

Android-first community app for The No Hands Company: discover projects, share development, discuss ideas, meet collaborators, and eventually offer products and services.

## Current state

- Product version: **0.0.0** — development baseline; Android foundation implemented, no released build.
- Documentation baseline: **DOC-0006**, 2026-10-03.
- First intended executable release: **0.0.1**, alpha; backend/accounts increment **0.0.2** is in review.
- Working product name: TNHC Community; final branding remains open.
- Primary platform: Android phones. No owner-funded Apple development or distribution. A community contributor may own a future iOS client and its distribution costs.
- Initial community: TNHC projects with member participation. Member project publishing and commerce are later stages.

## Product experience

Five implemented navigation destinations: Home, Projects, Community, Messages, Profile. Without local backend configuration the app shows a fictional offline preview; when configured it loads the public project catalogue from the self-hosted backend. The connected build includes invite-only sign-in, editable member profiles, password setup/change, sign-out and persistent project follow controls. Local backend and emulator checks for 0.0.2 pass; public deployment prerequisites remain; posting and messaging are later work. See [the 0.0.2 development record](docs/development/0.0.2-backend-accounts.md).

## Documentation index

| File | Purpose |
|---|---|
| [docs/BUILDING.md](docs/BUILDING.md) | Android Studio setup, build/test/install commands |
| [docs/development/0.0.1-foundation.md](docs/development/0.0.1-foundation.md) | Executed checks and remaining release gates |
| [docs/development/0.0.2-backend-accounts.md](docs/development/0.0.2-backend-accounts.md) | Current backend and Android progress, checks, and remaining work |
| [ROADMAP.md](ROADMAP.md) | Planned versions, scope, and completion gates |
| [VERSIONING.md](VERSIONING.md) | Version numbers and release rules |
| [CHANGELOG.md](CHANGELOG.md) | Actual changes; never planned delivery claims |
| [FEATURES.md](FEATURES.md) | Stable feature IDs, targets, and acceptance criteria |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Development and documentation workflow |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Product rules and scope |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Proposed components and unresolved decisions |
| [docs/BACKEND_ARCHITECTURE.md](docs/BACKEND_ARCHITECTURE.md) | Approved backend design, data domains, security, operations and release gates |
| [Backend implementation plan](docs/superpowers/plans/2026-10-03-backend-program.md) | Approved staged implementation plans for all backend milestones |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | Entities, ownership, and access |
| [docs/API_CONTRACT.md](docs/API_CONTRACT.md) | Stable client/backend routes, fields, pagination, errors and compatibility |
| [docs/TESTING.md](docs/TESTING.md) | Verification and release gates |
| [docs/SECURITY_AND_MODERATION.md](docs/SECURITY_AND_MODERATION.md) | Access, privacy, abuse handling |
| [docs/DEVTRACK.md](docs/DEVTRACK.md) | Tracking fields and export conventions |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Decision register |
| [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | Repeatable release procedure |
| [docs/releases/TEMPLATE.md](docs/releases/TEMPLATE.md) | Release record template |
| [docs/decisions/TEMPLATE.md](docs/decisions/TEMPLATE.md) | Architecture decision template |

`VERSION` is the canonical current product version. JSON and CSV files are proposed interchange formats, not confirmed DevTrack integrations.

## Getting started

Open this directory in Android Studio with JDK 17 or 21, SDK Platform 36 and Build-Tools 36.0.0. Build with `./gradlew :app:assembleDebug`; the APK is written to `app/build/outputs/apk/debug/app-debug.apk`. See [BUILDING.md](docs/BUILDING.md) for complete setup and tests.

Source repository: [The-No-Hands-company/tnhc-community](https://github.com/The-No-Hands-company/tnhc-community). The local accounts/follows implementation is in review; no public service is deployed. Distribution, final application ID and license remain undecided. Never commit credentials or signing keys.
