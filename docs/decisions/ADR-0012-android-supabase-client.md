# ADR-0012: Android Supabase client and session storage

- **Status:** Accepted for alpha 0.0.2 implementation
- **Date:** 2026-10-03
- **Decision owners:** TNHC

## Context

Android needs invite-only email/password authentication, session refresh,
PostgREST reads and writes, and the same API boundary planned for a future iOS
client. The client implementation must not expose service-role credentials or
make product UI depend on Supabase-specific classes.

Supabase's Kotlin documentation lists `auth-kt` and `postgrest-kt`, sets
Android's minimum SDK at API 26, and requires a Ktor engine. Supabase describes
the Kotlin client as community-maintained rather than official. The current
stable `supabase-kt` release is 3.8.0; it is built with Kotlin 2.4.0 and Ktor
3.5.1. The app can use Kotlin 2.4.20 with its existing AGP 8.13.2 and Gradle
8.13, within the compatibility range published by Kotlin.

## Decision

- Pin `supabase-kt` 3.8.0 and Ktor Android 3.5.1 in the version catalog.
- Upgrade the Kotlin Gradle plugin and Compose compiler plugin together to
  2.4.20. Keep the existing Gradle wrapper and Android Gradle Plugin unless
  build verification shows a compatibility issue.
- Use only `auth-kt` and `postgrest-kt` now. Add another Supabase module only
  when a feature requires it.
- Put Supabase calls behind `CommunityRepository`. UI and domain code depend
  only on TNHC models and suspend operations so the transport can be replaced
  without rewriting screens.
- Persist Auth sessions through a TNHC `SessionManager` that encrypts serialized
  session data with AES-GCM and an Android Keystore key. Exclude app data from
  backup. Never log session material.
- Configure the URL and publishable key through local Gradle configuration or
  environment. Treat the publishable key as public; never embed the service
  role key. Release builds require an HTTPS URL and configured publishable key.

## Consequences

The active library receives fixes and session handling, while the app accepts
its community-maintained status and pins it behind an internal interface.
Updating the Kotlin toolchain is required for the current client release.
`list_projects` is exposed through a bounded, RLS-respecting Postgres RPC so
the app can use opaque keyset cursors without encoding database order rules in
the UI.

## References

- [Supabase Kotlin installation and Android/API 26 requirements](https://supabase.com/docs/reference/kotlin/installing)
- [Supabase Kotlin client support and maintenance status](https://supabase.com/docs/reference/kotlin/introduction)
- [supabase-kt 3.8.0 release](https://github.com/supabase-community/supabase-kt/releases/tag/3.8.0)
- [supabase-kt 3.8.0 tool versions](https://github.com/supabase-community/supabase-kt/blob/3.8.0/gradle/libs.versions.toml)
- [Kotlin/Gradle/AGP compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
