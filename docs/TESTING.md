# Testing and verification

The Android foundation now has runnable unit and Compose device tests. See [BUILDING.md](BUILDING.md) for commands and the [0.0.1 development record](development/0.0.1-foundation.md) for actual outcomes. Six unit tests pass; device tests compile but have not run because the available emulator crashes before ADB connection. Compilation is not device verification.

Each feature needs evidence that its acceptance criteria pass. Record device/build, date, steps, outcome and defects. Automated checks are chosen for meaningful behavior and regression risks.

## Required coverage

- Account/session: sign-in, invalid credentials, expiry, sign-out, recovery, deletion.
- Permissions: member versus maintainer/moderator; another user's private resources; removed membership.
- Community: posting, editing, pagination, report handling, blocked interactions.
- Messaging: only participants can read/send; retries avoid duplicates; blocked-contact behavior.
- Android: navigation/back, process restart, interrupted network, text scaling, accessibility labels, notification routing.
- Data operations: migrations, backup restore, deletion outcomes and media access.

Use at least two accounts for isolation checks and representative Android devices/emulators chosen during stack setup. Use synthetic test data.

## Gates

Alpha: install and smoke checks for the delivered subset; limitations declared. Before external participants: working report/block and moderator response.
Beta: core journey, permission and privacy checks pass; no critical open defects; support and recovery are operational.
Stable: regression checks pass, remaining defects are triaged, operational ownership exists, restore/rollback procedure is rehearsed.

Performance acceptance values must be agreed and recorded before claiming them satisfied. A passing test count alone is not a release decision.
