# TNHC Community

Android-first community app for The No Hands Company: discover projects, share development, discuss ideas, meet collaborators, and eventually offer products and services.

## Current state

- Product version: **0.0.0** — development baseline; Android foundation implemented, no released build.
- Documentation baseline: **DOC-0002**, 2026-10-03.
- First intended executable release: **0.0.1**, alpha.
- Working product name: TNHC Community; final branding remains open.
- Primary platform: Android phones. No owner-funded Apple development or distribution. A community contributor may own a future iOS client and its distribution costs.
- Initial community: TNHC projects with member participation. Member project publishing and commerce are later stages.

## Product experience

Five implemented navigation destinations: Home, Projects, Community, Messages, Profile. This offline foundation supports demo project browsing, filtering and details. Following, posting, accounts and messaging are planned for later alphas. Community, Messages and Profile currently explain that upcoming scope. Official TNHC, community, and partner projects must be visibly distinguishable.

## Documentation index

| File | Purpose |
|---|---|
| [docs/BUILDING.md](docs/BUILDING.md) | Android Studio setup, build/test/install commands |
| [docs/development/0.0.1-foundation.md](docs/development/0.0.1-foundation.md) | Executed checks and remaining release gates |
| [ROADMAP.md](ROADMAP.md) | Planned versions, scope, and completion gates |
| [VERSIONING.md](VERSIONING.md) | Version numbers and release rules |
| [CHANGELOG.md](CHANGELOG.md) | Actual changes; never planned delivery claims |
| [FEATURES.md](FEATURES.md) | Stable feature IDs, targets, and acceptance criteria |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Development and documentation workflow |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Product rules and scope |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Proposed components and unresolved decisions |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | Entities, ownership, and access |
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

Source repository: [The-No-Hands-company/tnhc-community](https://github.com/The-No-Hands-company/tnhc-community). Backend hosting, distribution, final application ID and license remain undecided. Never commit credentials or signing keys.
