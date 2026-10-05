# TNHC Community invitation email setup

The Founder app calls the `invite-member` Edge Function. It checks the
signed-in account's server-owned administrator or Founder role, records an
expiring invitation, and asks Supabase Auth to send the one-use invitation
link. The Android app never connects to Resend and must never contain a mail
provider credential.

## Production SMTP settings

The public Community backend is a self-hosted Supabase Compose project. Keep
these Auth values in its private `.env` file:

| Environment variable | Value |
| --- | --- |
| `SMTP_HOST` | `smtp.resend.com` |
| `SMTP_PORT` | `465` |
| `SMTP_USER` | `resend` |
| `SMTP_PASS` | Resend API key restricted to `tnhc.dev` |
| `SMTP_ADMIN_EMAIL` | `no-reply@tnhc.dev` |
| `SMTP_SENDER_NAME` | `TNHC Community` |

Port 465 uses implicit TLS. Keep the Resend key out of Android build settings,
source control, application logs, and support messages. Resend must show
`tnhc.dev` as verified for sending.

Set the Auth redirect settings in the same private `.env`:

```dotenv
SITE_URL=https://tnhc.dev
API_EXTERNAL_URL=https://auth.tnhc.dev/auth/v1
ADDITIONAL_REDIRECT_URLS=tnhccommunity://invite
```

After changing SMTP or redirect settings, restart Auth:

```sh
backend/deploy/run-production.sh restart auth
```

The production deployment procedure, Cloudflare Tunnel origin, data volumes,
and Founder bootstrap are documented in
[`PRODUCTION_DEPLOYMENT.md`](PRODUCTION_DEPLOYMENT.md).

## Test the invitation flow

Sign in with the provisioned Founder account, send one invitation to an inbox
you can check, and confirm that it arrives. Open the one-use link on an Android
device with the app installed. A successful Auth response means SMTP accepted
the message for delivery; confirm the recipient inbox before treating it as
delivered.

Use a neutral sender such as `no-reply@tnhc.dev`; this address does not itself
create an inbox for replies. Resend's sending DNS records can coexist with
Cloudflare Email Routing when they use their dedicated `send` and DKIM names.
Do not replace the root (`@`) MX records for Community invitations.

## Keep local development captured

Do not enable external SMTP in the checked-in local
`backend/supabase/config.toml`. Local invitation tests should continue to
place mail in Mailpit. If a developer needs a live-provider smoke test, use an
isolated, uncommitted secret and a test recipient, then remove the credential.

This configures Supabase Auth email for the Community app. It does not
configure Nexus Email's separate outgoing message queue or Cloudflare inbound
mail bridge.
