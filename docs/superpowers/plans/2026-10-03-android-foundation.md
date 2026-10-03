# Android foundation implementation plan

> Execution: apply the executing-plans workflow inline; review and verify each deliverable before moving on.

**Goal:** Implement TNHC-001 and TNHC-002 for the 0.0.1 Android alpha target.

**Architecture:** Single Android application module. Compose UI consumes a replaceable project repository; immutable domain models and filtering remain independent of Android. Saved UI state controls the five destinations and project details.

**Tech stack:** Kotlin, Jetpack Compose, Material 3, Gradle, JUnit, AndroidX test.

**Spec:** [Android foundation design](../specs/2026-10-03-android-foundation-design.md).

## Global constraints

- Android only; minimum API 26, compile/target API 36.
- Product version 0.0.0 until executable release checks pass; target 0.0.1.
- Synthetic, explicitly labelled demo data; no network or real member information.
- Home, Projects, Community, Messages, Profile; later functionality clearly identified.
- Preserve feature IDs and synchronise Markdown/JSON/CSV tracking.

## 1. Build and catalogue foundation

- [x] Create root Gradle configuration, checked-in wrapper and app module; pin compatible dependency versions.
- [x] Add ProjectRepositoryTest for case-insensitive text/tag search, combined category filtering, no matches, empty input and unique IDs.
- [x] Attempt tests before implementation; distinguish missing behavior from unavailable tooling.
- [x] Implement Project.kt and ProjectRepository.kt with demo fixtures and pure filtering.
- [x] Run unit tests; retain failures and blockers as evidence.

Files: settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/*, gradlew, gradlew.bat, app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/main/java/com/tnhc/community/data/*, app/src/test/java/com/tnhc/community/data/*.

## 2. Native phone experience

- [x] Add Compose tests for five destinations, project detail/back, query reset, restoration and load failure/retry.
- [x] Implement MainActivity, theme, app shell and separate catalogue/detail/home/holding screens.
- [x] Save tab, detail ID and filters; handle Android Back and missing project IDs.
- [x] Build debug APK, run lint and unit tests; compile and run device tests where tooling is available.
- [ ] Inspect the native rendered UI: blocked by emulator startup segmentation fault; see development record.

Files: app/src/main/java/com/tnhc/community/MainActivity.kt, app/src/main/java/com/tnhc/community/ui/*, app/src/main/res/*, app/src/androidTest/java/com/tnhc/community/*.

## 3. Developer handoff and tracking

- [x] Add BUILDING.md with SDK/JDK setup, reproducible build/test/install commands and APK location.
- [x] Record stack decision, scope, test evidence and remaining release gates.
- [x] Update README, ROADMAP, CHANGELOG, FEATURES, project.json and both feature exports without claiming release.
- [x] Validate metadata consistency, inspect source changes and report the runnable result and concrete limitations.
