# Building the Android app

The first implementation targets alpha **0.0.1**. The current product version in `VERSION` remains **0.0.0** until release checks pass. Debug builds use application ID `com.tnhc.community.debug`, separate from the provisional release ID `com.tnhc.community`.

## Requirements

- Android Studio with a JDK 17 or 21 Gradle runtime.
- Android SDK Platform 36 and Build-Tools 36.0.0.
- Platform-Tools for installation; an Android phone or emulator running API 26 or later.
- Internet access for the first Gradle dependency download. The application itself works offline and requests no permissions.

Open this directory in Android Studio and let Gradle sync. Select the `app` configuration and an Android device, then Run. Set the Gradle JDK to 17 or 21. The checked-in Gradle wrapper downloads Gradle 8.13 and verifies its distribution checksum.

For command-line builds, set `ANDROID_HOME` to your SDK directory, or add `sdk.dir=/absolute/path/to/your/sdk` to the ignored `local.properties`. Do not commit local SDK paths, signing keys or passwords.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows, use `gradlew.bat`. The connected test command requires a booted emulator or an attached phone with USB debugging enabled.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tnhc.community.debug/com.tnhc.community.MainActivity
```

Debug APKs are signed automatically with a development key. They are for local testing, not public distribution. A production signing identity, final application ID, distribution route and license remain separate decisions. No store fees or Apple tooling are required for this local development workflow.

## Source map

| Path | Responsibility |
|---|---|
| `app/src/main/java/com/tnhc/community/MainActivity.kt` | Android entry point and system insets |
| `app/src/main/java/com/tnhc/community/data/` | Project model, repository and local filtering |
| `app/src/main/java/com/tnhc/community/ui/` | Theme, navigation and screens |
| `app/src/test/` | Catalogue behavior tests |
| `app/src/androidTest/` | Native navigation, restoration and failure recovery tests |
| `gradle/libs.versions.toml` | Pinned dependency versions |
| `VERSION` | Canonical current product version, read into Android versionName |

The repository intentionally contains four fictional demo projects. Replace them only with a verified inventory; do not infer the owner's real projects from sample names. The in-memory repository is synchronous because fixtures are tiny and local. A future disk/network implementation must introduce asynchronous loading and lifecycle-aware state before doing I/O.

English copy and system light/dark themes are included. Localization, production account flows, follow persistence, posting, messaging and commerce are not implemented. See the roadmap for their targets.

## Validation records

See [the 0.0.1 development record](development/0.0.1-foundation.md) for executed checks and outstanding release gates. Generated reports are under `app/build/reports/`. GitHub Actions builds the APK and runs unit tests and Android lint; connected tests run separately on a device/emulator.

## References

Kotlin's Compose compiler plugin is configured following [Android's Compose setup guidance](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler). The Kotlin and Compose compiler plugin versions match. Gradle and Android library versions are pinned rather than dynamically selected.
