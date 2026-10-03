# ADR-0010: Native Android foundation

- Date: 2026-10-03
- Status: Accepted for the local alpha implementation
- Features: TNHC-001, TNHC-002

## Context

The owner approved an Android-first mobile app and asked to start implementation. The first milestone needs a native shell and a demo catalogue; there is no existing application stack. Apple development and distribution are outside the owner's funded scope.

## Decision

Use Kotlin with Jetpack Compose and Material 3. Minimum SDK 26; compile and target SDK 36. Use a single app module with a separate project model/repository boundary and saved UI navigation state. No backend or networking is required for the foundation. Keep future API contracts client-independent.

Use Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin/Compose compiler 2.2.21 and Compose BOM 2025.12.00. Pin Build-Tools 36.0.0. The application ID is provisionally `com.tnhc.community`; debug builds append `.debug`. Current development versionName is read from VERSION; development versionCode is 1 and has not been allocated to a distributed release.

## Alternatives

Flutter and React Native offer shared UI across platforms but add a cross-platform dependency before an iOS contributor exists. Web/PWA delivery does not match the requested native Android deliverable. Kotlin/Compose directly fits the current platform priority.

## Consequences

An eventual iOS contributor will implement a client against shared backend contracts. Accounts, persistence and asynchronous remote loading remain future work. Confirm the application ID, signing ownership and distribution channel before publication. The SDK floor is an initial support choice; test coverage must be expanded before claiming support across all API 26+ devices.

The five previously proposed navigation destinations are adopted for this foundation. Home/Projects are functional; Community/Messages/Profile disclose their future scope. No feature is marked released merely because a holding screen exists.

## Design tooling

The 12ui draft service returned a corpus-mode mismatch before producing candidate screens. The implementation uses standard native Material components and a local light/dark theme; no generated design or pixel-match claim is made.
