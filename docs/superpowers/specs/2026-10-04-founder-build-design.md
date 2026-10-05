# TNHC Founder build and control plane

**Status:** Design approved in chat on 2026-10-04; implementation and local verification recorded, device verification pending.

## Purpose

Provide the TNHC founder with a separate Android build that exposes direct
control of in-app community and application settings. The regular member app
remains focused on member workflows. The Founder build is a separate client
surface, not a source of authority: the backend confirms the signed-in
account's Founder role for every privileged read and write.

## Build and identity

Android will have `member` and `founder` product flavors. Each flavor has a
distinct application ID; the Founder flavor has its own name and launcher
identity. A compile-time flag lets the Founder flavor include Founder screens
and navigation. It grants no server permission. A member can install or
inspect the Founder APK and still receives no Founder data or actions.

The backend adds a `founder` platform role. It is provisioned and transferred
only through an out-of-band server/database operator procedure. Neither an
Android client, invitation function, ordinary administrator, nor migration
seed may grant or remove it. There is one Founder at a time. If Founder access
is lost, the operator recovery procedure is the break-glass path; no embedded
recovery secret is added to either APK.

## Founder controls

The Founder console is the in-app control plane for:

- inviting members and reviewing, activating, suspending, or closing accounts;
- assigning platform administrator/moderator roles and project memberships;
- creating and maintaining projects, topics, and their visibility;
- reviewing, hiding, restoring, or removing posts and comments;
- changing allow-listed application settings such as maintenance notice and
  feature availability.

The controls are added in domain slices alongside each backend capability.
The Founder has system-wide management authority over those in-app records.
Private communications remain governed by the documented report-scoped review
policy; Founder status does not create general message-body access.

## Authorization, audit, and failure behavior

The server remains the final authorization boundary. Database RLS and narrow
server functions check the Founder role for each action. The Founder role
cannot be asserted by client metadata, a build flag, local storage, or an
untrusted request field. Regular member and moderator policies remain
least-privileged.

Every successful Founder mutation records the authenticated actor, action,
target type and stable target ID, timestamp, and a redacted summary in an
append-only audit log. Credentials, tokens, invitation links, private message
bodies, and report evidence are excluded. Failed authorization returns a
sanitized denial; the Founder UI offers retry for network failures and never
assumes a mutation succeeded without a server response.

If the Founder role cannot be loaded, Founder controls stay unavailable. The
Founder build can still perform ordinary member workflows when signed in as a
member. There is no client-side offline override for privileged actions.

## Application settings and infrastructure boundary

Application settings are typed and allow-listed. The first controls cover a
global maintenance notice and feature availability; arbitrary code, SQL,
secrets, host commands, and server credentials are not editable through the
app. Server provisioning, host updates, backup management, DNS, certificates,
and incident recovery stay in the operator runbook, outside the Android app.

## Rollout and verification

The Founder build is distributed privately and is not selected as the public
member artifact. Both flavors use the same API contract and public client
configuration. No privileged credential or signing key is embedded in either
APK.

Verification must prove:

- flavor IDs, names, and navigation are distinct, while both builds compile;
- a Founder account sees controls and can perform authorized operations;
- a regular account installed in the Founder build sees no controls or data;
- a regular build cannot call privileged operations, including by direct API
  request;
- ordinary administrators cannot grant, remove, or impersonate the Founder;
- a migration or service restart preserves the sole Founder assignment;
- every successful Founder change has an audit record with sensitive values
  redacted;
- private communications remain inaccessible except for the documented
  report-scoped review path.

The Founder control plane is implemented domain by domain: identity and roles,
project/topic management, content moderation, then application settings. It
does not expand the alpha release gate to public deployment; existing HTTPS,
SMTP, backup, recovery, and operations prerequisites still apply.
