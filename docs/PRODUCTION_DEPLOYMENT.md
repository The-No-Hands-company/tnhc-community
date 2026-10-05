# Community production deployment

The public Community backend is a separate self-hosted Supabase Compose
project. Never expose the local development stack or its Mailpit service.

## Host configuration

Keep the official Supabase self-hosting checkout and its `.env` outside the
Git repository. Set `TNHC_SUPABASE_STACK_DIR` to that checkout before using
[`../backend/deploy/run-production.sh`](../backend/deploy/run-production.sh).
Keep the `.env` file readable only by the deployment account. It contains
generated database/JWT credentials and Auth settings documented in
[`INVITATION_EMAIL_SETUP.md`](INVITATION_EMAIL_SETUP.md).

The Cloudflare Tunnel's published application route for `auth.tnhc.dev` must
target `http://172.17.0.1:8000`. Only the API gateway is bound to that Docker
bridge address. Database and pooler ports must not be published to the
internet.

## Start and update

The checked-in production data-volume override stores Postgres and Storage
data in persistent Docker volumes. This is required because the Supabase
checkout is on a filesystem without POSIX ownership and permission support.

```sh
export TNHC_SUPABASE_STACK_DIR=/path/to/self-hosted-supabase
backend/deploy/run-production.sh up -d --wait
```

Apply ordered files from `backend/supabase/migrations/` to the production
database and record each applied version in
`supabase_migrations.schema_migrations`. Do not apply `seed.sql` to production;
it contains synthetic local fixtures. Copy
`backend/supabase/functions/invite-member/` into the stack's
`volumes/functions/invite-member/` before starting or updating Edge Functions.
Keep `VERIFY_JWT` enabled.

Create the initial Founder account through Auth Admin, then assign its
server-owned `founder` role through the private database connection after
verifying account ownership. The Founder APK alone never grants that role.
The service-role key and database password must never enter an APK.

## Android client configuration

Put only these client values in the ignored `local.properties` for a local
Founder build, or supply them as Gradle properties in a release pipeline:

```properties
TNHC_BACKEND_URL=https://auth.tnhc.dev
TNHC_PUBLISHABLE_KEY=<Supabase publishable key>
```

The publishable key is intended for clients. Never substitute the
service-role key. The app invitation link returns through
`tnhccommunity://invite`.

Verify `https://auth.tnhc.dev/auth/v1/health` with a client-like request, then
test one invitation end to end. A successful Auth response means the message
was accepted for SMTP delivery; confirm inbox delivery and open the link on a
device with the app installed before treating mail as operational.
