# Android foundation — target 0.0.1

This implements the approved Android-first scope and ROADMAP.md foundation milestone (TNHC-001 and TNHC-002). Product version stays 0.0.0 until the executable alpha passes its release gate. No release or distribution is implied by source implementation.

## Implementation decision

Use a native Kotlin Android application with Jetpack Compose and Material 3. A native client fits the Android-first scope and requires no Apple tooling. Flutter or React Native would share more UI with a future iOS client but add a cross-platform toolchain before that client has an owner. Keep project data behind a repository boundary; the future backend contract remains independent of the Android UI.

Minimum Android version: Android 8.0 (API 26). Compile and target API 36. Package: `com.tnhc.community`, provisional until publication. No backend, account creation, permissions, analytics or network requests in this milestone.

## Experience

Five destinations: Home, Projects, Community, Messages, Profile. Home introduces TNHC and links to featured demo projects. Projects provides a local catalogue, text and category filters, a resettable empty state, and individual project pages. Each detail includes name, description, stage, affiliation, tags and current focus. All sample projects are fictional and visibly marked as demo content; none asserts a real import of the 300+ project inventory.

Community, Messages and Profile contain useful explanatory holding screens with their planned versions and a working route to browse projects. Do not simulate sending messages, signing in, following or buying. Those belong to later features.

Back closes project details before leaving their originating tab; from another main tab it returns Home; from Home Android can exit normally. Selected tab, selected project, query and filter survive activity recreation via saved state. Lists are scrollable; interactive elements have accessible labels and standard Material touch targets. Support light/dark system themes, font scaling and system insets.

## Data and failure behavior

`ProjectRepository` supplies immutable `Project` models. `DemoProjectRepository` supplies synthetic fixtures. Search is local and case-insensitive across title, summary and tags; category and text filters combine. Empty data, no filter matches, load failure and unknown project IDs have distinct recovery text/actions. Loading failure offers retry; empty search offers clear filters. This local filter is not completion of the later platform-wide search feature.

## Verification

Unit tests exercise filtering, stable unique fixture IDs, empty input and repository failures. Compose instrumentation tests exercise all tabs, project entry/back, saved-state recreation, filter empty/reset, and failure/retry using an injected repository. Build a debug APK and run lint/unit tests. Device installation, navigation, TalkBack, font scaling and system Back remain release gates until actually executed on an Android device/emulator. Record exact checks and limitations in docs/TESTING.md and a development record.

## Tracking

Update FEATURES.md, features.json and features.csv together. Implemented-but-unverified features remain in_progress or in_review, with owner and evidence; introduced_version stays empty until distribution. VERSION and project.json retain the current product version; build metadata separately records the 0.0.1 target and development versionCode. No paid services or publication are part of this change.
