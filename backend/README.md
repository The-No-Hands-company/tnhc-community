# TNHC Community backend

This directory contains the local Supabase development stack and versioned
database migrations. The stack is for development and tests; it is not the
public pilot server.

## Tool versions

- Supabase CLI: **2.119.0** (pinned in CI and local commands below).
- Deno: **2.9.6** (pinned for Edge Function tests in CI).
- Docker Engine: **29.6.2** was available during initial setup. Supabase local
  development requires a working Docker-compatible engine; use a current
  supported release and keep it patched.

Install Docker Engine/Desktop using the vendor instructions for your operating
system. Install Node.js and npm, then run the CLI through `npx` so no global
installation or unpinned CLI is needed.

## Start locally

From the repository root:

```sh
npx --yes supabase@2.119.0 start --workdir backend
npx --yes supabase@2.119.0 status --workdir backend
```

Local Auth and PostgREST use `http://127.0.0.1:54321` (Auth at
`/auth/v1`, REST at `/rest/v1`). The local database listens on port 54322 and
Studio on port 54323. These development endpoints must not be exposed to the
internet. The CLI prints local publishable/anon credentials; keep the local
configuration outside release artifacts.

To apply migrations and synthetic fixtures from an empty database, or to run
database tests:

```sh
npx --yes supabase@2.119.0 db reset --workdir backend
npx --yes supabase@2.119.0 test db --workdir backend
```

Stop containers when finished:

```sh
npx --yes supabase@2.119.0 stop --workdir backend
```

All schema changes belong in ordered files under `supabase/migrations/`.
`seed.sql` may contain only synthetic, clearly labelled development fixtures.
Never commit `.env`, Docker volumes, generated signing keys, database
credentials, service-role keys or real member data. See
[`../docs/BACKEND_ARCHITECTURE.md`](../docs/BACKEND_ARCHITECTURE.md) for the
deployment and security boundary and [`../docs/API_CONTRACT.md`](../docs/API_CONTRACT.md)
for client-facing behavior.

## Invitation function

The pilot has open registration disabled. `invite-member` accepts requests
only from an authenticated administrator or Founder, creates an expiring invitation and
uses Auth Admin to send its one-use link. Local Auth messages are captured by
Mailpit at `http://127.0.0.1:54324`; external invitations require a configured
and tested SMTP provider. Never send invitations from an unverified public
instance.

For Resend configuration in the hosted Community project, follow
[`../docs/INVITATION_EMAIL_SETUP.md`](../docs/INVITATION_EMAIL_SETUP.md). The
Android app does not send mail itself: Auth sends the one-use invite email
through the SMTP provider configured on the server. Keep local development on
Mailpit; the checked-in local stack is not the public pilot backend.

Serve the function locally with the CLI-provided local keys:

```sh
npx --yes supabase@2.119.0 functions serve invite-member --workdir backend
```

Run its dependency-injected tests from the repository root:

```sh
npx --yes deno@2.9.6 test backend/supabase/tests/functions/invite-member.test.ts
```

The database tests also verify generated pending profiles, invite confirmation,
single-use acceptance, and rejection of expired invites.

## Founder account provisioning

The Founder APK does not grant a role. A trusted database operator provisions
the already-invited founder account out of band after verifying its Auth UUID.
Run this SQL through the private database administration connection:

```sql
insert into public.platform_roles (user_id, role)
values ('<verified-auth-user-uuid>', 'founder')
on conflict (user_id, role) do nothing;
```

The database permits one Founder assignment. A unique conflict means another
account already holds it; verify ownership before changing that row. Recovery
or transfer uses the same operator-only connection to remove the existing
`founder` assignment and add the verified account. Never provision Founder
through Android, Auth user metadata, an invitation, or an ordinary admin
operation. Do not place the UUID, database password, or service-role key in an
APK or committed configuration.

Invitation emails return to `tnhccommunity://invite` by default. To use another
client, configure `INVITE_REDIRECT_URL` in the function environment and add that
exact URL to Auth's redirect allowlist. The function passes the return URL in
the Auth request's `redirect_to` query parameter.

## Android invitation and restart check

Use a dedicated emulator. Build with the local publishable key and
`TNHC_BACKEND_URL=http://127.0.0.1:54321`; the script installs both local APKs,
then establishes an ADB reverse port forward to the local stack. It clears
the debug app's data on the selected emulator, creates synthetic local accounts,
accepts a generated invitation, and runs two separate instrumentation processes
to verify profile/follow retries and encrypted session restoration. It then
signs out and removes its own backend fixtures. It does not send email.

```sh
python3 backend/scripts/verify_android_session.py --supabase /path/to/supabase
```

The optional `--adb` and `--serial` flags select the SDK tool and emulator.
This check requires the local CLI stack; it refuses non-loopback backend URLs
and physical-device serials. It verifies Auth's generated invitation flow;
external SMTP delivery remains a separate deployment prerequisite.
