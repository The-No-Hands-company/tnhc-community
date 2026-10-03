# Contributing

Android setup and commands: [docs/BUILDING.md](docs/BUILDING.md). Run unit tests, lint and APK compilation before submitting app changes; run connected tests when a phone/emulator is available and report unavailable checks explicitly.

1. Select or create a stable feature ID; define user behavior and acceptance criteria.
2. Record owner, priority, dependencies, target version, and status. Use proposed branch name `feature/TNHC-001-short-description` or `fix/TNHC-001-short-description` when Git is set up.
3. Make a reviewable change; include access/error cases and appropriate verification.
4. Update impacted documentation in the same change. Use a decision record for changes to architecture, ownership, platform, or scope.
5. Review against acceptance criteria. Move to verified only with evidence. Set released and introduced_version only after distribution.
6. Update VERSION, changelog and release record together at release time; create the immutable version tag after the checks pass.

Allowed statuses: planned, ready, in_progress, blocked, in_review, verified, released, deferred. Blocked items need a reason; in_progress items need an owner. Optional features can be deferred explicitly.

Bug records use BUG-0001 onward and retain severity, reproduction, expected/actual behavior, affected versions, resolution and verified fixed version. Decisions use ADR-0001 onward. Never renumber or recycle IDs.

Do not add secrets or real member data to fixtures, commits, screenshots, or logs. Repository license and contributor terms must be decided before accepting externally supplied code for distribution.
