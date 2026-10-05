# TNHC Community invitation email setup

The Founder app calls the `invite-member` Edge Function. That function checks
the signed-in account's server-owned administrator or Founder role, records an
expiring invitation, and asks Supabase Auth to send its one-use invitation
link. The Android app never connects to Resend and must never contain a mail
provider credential.

## Configure the hosted Supabase project

The repository's Supabase configuration is for local development. Local Auth
captures messages in Mailpit and does not deliver them to real recipients. For
public invitations, configure the **hosted project used by the release APK**:

1. In Resend, confirm that the sending domain `tnhc.dev` is verified and create
   an API key with sending permission. Store the key securely; it is the SMTP
   password. The sender domain must be verified in the same Resend account.
2. In the Supabase Dashboard, select that hosted project and open
   **Authentication → Emails → SMTP Settings**. Enable custom SMTP and set:

   | Setting | Value |
   | --- | --- |
   | SMTP host | `smtp.resend.com` |
   | SMTP port | `465` |
   | SMTP username | `resend` |
   | SMTP password | The Resend API key |
   | Sender email | `no-reply@tnhc.dev` |
   | Sender name | `TNHC Community` |

   Port 465 is Resend's implicit-TLS SMTP connection. Do not use the Resend key
   as the username, and do not put it in Android build configuration, source
   control, application logs, or a support message.
3. In the same hosted project's Auth URL configuration, allow the exact
   invitation redirect `tnhccommunity://invite`. The current Android Auth
   client expects that deep link.
4. Build the release APK with the hosted HTTPS Supabase URL and its publishable
   key. The service-role key and Resend key stay on the server.
5. Sign in with the Founder account, send an invitation to an address you can
   check, and confirm both that the email arrives and that Resend records the
   send. Then complete the one-use link on an Android device with the app
   installed.

Resend accepting a message means it entered its delivery pipeline; check the
recipient inbox and Resend's delivery status before treating the invitation as
delivered. Use a neutral sender such as `no-reply@tnhc.dev`; the sender address
does not itself create an inbox for replies.

## Preserve Cloudflare's incoming-mail routing

Resend sending DNS records can coexist with Cloudflare Email Routing when they
are on the dedicated `send` and DKIM names shown by Resend. Do not replace the
root (`@`) MX records for the purpose of sending Community invitations. Those
records control incoming mail and are separate from the app's Supabase Auth
SMTP settings.

## Keep local development captured

Do not enable external SMTP in the checked-in local `backend/supabase/config.toml`.
Local invitation tests should continue to place mail in Mailpit. If a developer
needs a live-provider smoke test, use an isolated, uncommitted local secret and
a test recipient, then remove the credential afterward.

## What this setup does not configure

This enables Supabase Auth emails for the Community app. It does not configure
Nexus Email's separate outgoing message queue or make Nexus Email receive
messages from Cloudflare Email Routing. Those require their own server-side
SMTP relay and inbound bridge configuration.
