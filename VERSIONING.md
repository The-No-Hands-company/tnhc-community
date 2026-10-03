# Versioning policy

This project uses the owner's three-number release-stage convention. It is not standard Semantic Versioning during pre-release development.

| Version | Meaning | Examples |
|---|---|---|
| 0.0.0 | Planning; no executable release | Current documentation baseline |
| 0.0.N | Alpha releases | 0.0.1, 0.0.2, 0.0.3 |
| 0.B.P, B >= 1 | Beta releases | 0.1.0, 0.1.1, 0.2.0 |
| M.B.P, M >= 1 | Stable releases | 1.0.0, 1.0.1, 1.1.0, 2.0.0 |

## Increment rules

- Alpha: increment the final number for every shipped alpha, including fixes. 0.0.9 progresses to 0.0.10.
- Beta: 0.1.0 is the first beta. Increment the final number for corrections; increment the middle number and reset the final number for a new beta feature milestone.
- Stable: 1.0.0 is the first major public release. Increment the final number for fixes, middle for compatible additions, and first for incompatible changes or a deliberate major product milestone.
- A stage transition requires its roadmap gate to pass; the number alone is not evidence of readiness.
- Never reuse a published version or move its Git tag. A rollback deploys a known previous artifact; a corrected build receives a new version.

## Identity and traceability

Use `VERSION`, release heading, Git tag `vX.Y.Z`, and Android `versionName` consistently. Android `versionCode` is a separate monotonically increasing integer for every distributed build; maintain its allocation in release records. Example only: versionName 0.0.1, versionCode 1.

Product version stays 0.0.0 until an executable alpha passes release checks. Documentation changes use Git commits and documentation revisions such as DOC-0001, DOC-0002; they do not pretend that an app has shipped. API contracts and data migrations receive independent identifiers such as API-0001 and MIG-0001, mapped to the app release.

For every delivered feature retain: stable ID, target version, actual introduced version, change summary, issue/commit references, verification evidence, and release record. Target versions can move; introduced versions reflect actual delivery and remain historical facts.

Every release captures the app commit, backend commit/version, API contract, migrations, build code, release date, known limitations, and rollback procedure. No compatibility promise exists merely because two clients share a product version.
