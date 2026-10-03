# DevTrack tracking contract

DevTrack's actual import schema/API has not been provided. `project.json`, `features.json`, and `features.csv` are portable proposed formats; no automatic integration or successful import is claimed.

## Project fields

project_id: TNHC-COMMUNITY; name: TNHC Community; platform: android; version: 0.0.0; stage: development; target_version: 0.0.1; documentation_revision: DOC-0002; updated_at: 2026-10-03.

## Feature fields

id, title, status, priority, target_version, introduced_version, owner, dependencies, acceptance_criteria, evidence, issue_url, commit_sha, updated_at.

Empty introduced_version means not released. TNHC-001 and TNHC-002 are implemented and in_review; all other features remain planned. Dependencies identify stable feature IDs. JSON dependency/criteria fields are arrays; CSV uses semicolons for array cells. Dates are ISO 8601. Release history must retain actual versions even when future targets move.

## Updating records

Update FEATURES.md and JSON/CSV together in the same commit. At baseline they describe the same IDs, titles, targets and statuses. Once DevTrack integration is implemented, choose one authoritative source and generate the other views; do not permit competing editable copies to drift.

Suggested dashboard: current version/stage, target release, planned/in-progress/blocked/verified/released counts, milestone gates, open critical bugs, recent releases and documentation revision.
