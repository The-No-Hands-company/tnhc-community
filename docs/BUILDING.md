# Building the Android app

The current product version in `VERSION` remains **0.0.0** until release checks pass. Alpha 0.0.3 work adds a separate Founder build; the member debug application ID remains `com.tnhc.community.debug`, and the Founder debug ID is `com.tnhc.community.founder.debug`.

## Requirements

- Android Studio with a JDK 17 or 21 Gradle runtime.
- Android SDK Platform 36 and Build-Tools 36.0.0.
- Platform-Tools for installation; an Android phone or emulator running API 26 or later.
- Internet access for the first Gradle dependency download. Configured backend builds need network access at runtime; the unconfigured preview works offline.

Open this directory in Android Studio and let Gradle sync. Select the `app` configuration and an Android device, then Run. Set the Gradle JDK to 17 or 21. The checked-in Gradle wrapper downloads Gradle 8.14.4 and verifies its distribution checksum.

For command-line builds, set `ANDROID_HOME` to your SDK directory, or add `sdk.dir=/absolute/path/to/your/sdk` to the ignored `local.properties`. Do not commit local SDK paths, signing keys or passwords.

## Local backend connection

Start the local Supabase stack as described in [`../backend/README.md`](../backend/README.md). For a standard Android emulator, `10.0.2.2` reaches host services. In restricted emulator environments that cannot route to `10.0.2.2`, use `127.0.0.1` and run `adb reverse tcp:54321 tcp:54321` first. Add the matching local URL to ignored `local.properties`, with the local publishable key printed by the CLI; never use a service-role key.

```properties
TNHC_BACKEND_URL=http://10.0.2.2:54321
TNHC_PUBLISHABLE_KEY=<local-publishable-key>
```

`10.0.2.2` routes from the Android emulator to services on the development computer. Physical phones need a reachable computer address on the same network and a deliberate firewall/router setup; do not expose the development backend publicly. Without these two settings, debug builds launch the clearly labelled fictional demo preview. Release builds require an HTTPS backend URL and a publishable key.

```sh
./gradlew :app:verifyBothDebugApps
./gradlew :app:connectedMemberDebugAndroidTest :app:connectedFounderDebugAndroidTest
```

On Windows, use `gradlew.bat`. The connected test command requires a booted emulator or an attached phone with USB debugging enabled. If a headless emulator segfaults under SwiftShader, start it with `-gpu host -feature -Vulkan`.

`verifyBothDebugApps` runs Member and Founder unit tests and lint, compiles both instrumentation-test suites, and builds both APKs from the same checkout. GitHub Actions runs this task on pushes and pull requests, then publishes the two APKs together under an artifact named for the commit SHA.

With a phone connected and USB app installs allowed by Android, install both builds from the same checkout with:

```sh
./gradlew :app:installBothDebugApps
```

The builds remain separate installed apps so Founder controls stay in the private Founder package. A Git push updates source control; installing the paired APKs updates the apps on a device.

Member debug APK: `app/build/outputs/apk/member/debug/app-member-debug.apk`.
Founder debug APK: `app/build/outputs/apk/founder/debug/app-founder-debug.apk`.

```sh
adb install -r app/build/outputs/apk/member/debug/app-member-debug.apk
adb install -r app/build/outputs/apk/founder/debug/app-founder-debug.apk
adb shell am start -n com.tnhc.community.founder.debug/com.tnhc.community.MainActivity
```

The Founder flavor adds its private Founder navigation and console to the same
shared community app. The server must independently confirm the signed-in
account's single `founder` role before the console is shown. Provision and
recover that role only through the operator procedure in
[`../backend/README.md`](../backend/README.md). Neither
APK contains service-role credentials or a Founder secret. Founder actions
apply to in-app members, projects, topics, posts/comments and allow-listed app
settings; general private-message access and host/deployment operations stay
outside the app.

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

The repository intentionally contains four fictional demo projects. Replace them only with a verified inventory; do not infer the owner's real projects from sample names. When local backend settings are present, the app loads the public catalogue through its cursor-based RPC and enables invite-only sign-in, profile editing, password setup/change, sign-out and project follows.

English copy and system light/dark themes are included. Public backend deployment, end-to-end account testing, posting, messaging and commerce are not complete. See the roadmap for their targets. The current debug APK is built without backend credentials and therefore opens in the fictional offline preview; configure the local URL and publishable key to test account flows.

## Validation records

See [the 0.0.1 development record](development/0.0.1-foundation.md) for executed checks and outstanding release gates. Generated reports are under `app/build/reports/`. GitHub Actions builds the APK and runs unit tests and Android lint; connected tests run separately on a device/emulator.

## References

Kotlin's Compose compiler plugin is configured following [Android's Compose setup guidance](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler). The Kotlin and Compose compiler plugin versions match. Gradle and Android library versions are pinned rather than dynamically selected.
