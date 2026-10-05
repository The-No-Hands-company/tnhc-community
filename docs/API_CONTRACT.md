# TNHC Community API contract

**Contract version:** 1 (planning baseline; backend target 0.0.2)

This document defines the compatibility boundary for Android and future
clients. Supabase Auth, PostgREST, and narrowly scoped Edge Functions provide
the transport. PostgreSQL migrations are authoritative for schema. Clients
must use the public publishable key plus a user access token; elevated server
keys never belong in an app.

## Common behavior

- Local development base URL: `http://127.0.0.1:54321`. Production must use a
  verified HTTPS origin. Auth routes are under `/auth/v1`, table and RPC routes
  under `/rest/v1`, and privileged workflows under `/functions/v1`.
- Send `apikey: <publishable-key>` on Supabase requests. Send
  `Authorization: Bearer <access-token>` for member-only operations. The
  public project catalogue may be read with the publishable key alone.
- JSON uses UTF-8, snake_case field names, ISO-8601 UTC timestamps and UUID
  identifiers. Unknown response fields must be ignored by clients.
- Do not infer authorization from hidden UI. RLS and server-side function
  checks are authoritative.
- Lists have a maximum page size of 50. Omitted limit defaults to 20. A cursor
  is an opaque, URL-safe value issued by the server; clients must not inspect
  or construct it. `next_cursor` is `null` at the end. Ordering is stable for
  one query and ties are broken by immutable UUID.
- Errors use the Supabase/PostgREST error envelope `{ "code": string,
  "message": string, "details": string|null, "hint": string|null }` where
  applicable. Clients branch on stable HTTP status and `code`, never parse
  human-readable `message` text.

## Authentication

Open sign-up is disabled. Accounts are created only through an expiring,
single-use administrator invitation. The recipient follows the Auth invite
link, confirms the invited address, then sets a password with at least 12
characters before signing in normally. The authenticated `updateUser` call
sets the password; no sign-up endpoint is exposed to the client.

- `POST /auth/v1/token?grant_type=password` with `{ "email", "password" }`
  signs in an active, confirmed member. Success returns Auth's
  `{ access_token, refresh_token, expires_in, token_type, user }` session.
- `POST /auth/v1/token?grant_type=refresh_token` with
  `{ "refresh_token" }` rotates the session. Clients replace both tokens
  atomically and discard the old refresh token.
- `POST /auth/v1/logout` invalidates the current session. Clear local tokens
  even if the network request fails.
- `POST /functions/v1/invite-member` accepts `{ "email": string }` only from
  an authenticated administrator or Founder. Success is `{ "invitation_id": UUID,
  "status": "sent" }`. The function never returns an Auth confirmation
  token. Email delivery depends on configured SMTP.

The role endpoint exposes only the caller's own assignments:
`GET /rest/v1/platform_roles?user_id=eq.<caller-uuid>&select=role`. The Founder
build may show its Founder console only after this authenticated response
contains `founder`. Build flavor and client metadata grant no permission. The
Founder assignment is provisioned and recovered only by a trusted database
operator; clients and ordinary administrators cannot create or remove it.

## Profiles

`GET /rest/v1/profiles?id=eq.<uuid>&select=id,handle,display_name,bio,interests,visibility,account_state,created_at,updated_at`
returns the caller's profile or a profile visible under its visibility rule.
Profile fields are:

- `id`: Auth user UUID; stable and immutable.
- `handle`: unique lowercase account handle.
- `display_name`: display label, nullable until configured.
- `bio`: plain text, nullable.
- `interests`: array of plain-text interest labels.
- `visibility`: `members` or `private`.
- `account_state`: `pending`, `active`, `suspended`, or `closed`.
- `created_at`, `updated_at`: UTC timestamps.

`PATCH /rest/v1/profiles?id=eq.<caller-uuid>` accepts only
`{ "display_name", "bio", "interests", "visibility" }`. IDs, handle, role
and account state are server-controlled. Success returns the updated profile
when `Prefer: return=representation` is supplied. Other-member edits fail with
403; private profiles of other members are not disclosed.

## Projects

`POST /rest/v1/rpc/list_projects` accepts `p_cursor` (nullable opaque cursor),
`p_limit` (1–50), `p_stage`, `p_owner_category`, and `p_tag` (all filters
nullable). The server returns a bounded page and `next_cursor`; clients pass
that cursor back unchanged. Results are ordered by `updated_at` descending,
then `id` ascending. Each item contains:

`id`, `slug`, `owner_category` (`official`, `community`, `partner`), `title`,
`summary`, `stage`, `tags`, `visibility`, `created_at`, and `updated_at`.

Only public projects are visible without authentication. Member-only projects
require an active account and applicable membership; private projects require
explicit access. Project maintainers and administrators use the project
operations documented below. Founder-only project maintenance uses audited
server functions. Ordinary clients cannot set trusted ownership or role fields.

Response envelope:

```json
{
  "items": [{"id":"<uuid>","slug":"sample","owner_category":"official","title":"Synthetic demo project","summary":"Development fixture","stage":"prototype","tags":["demo"],"visibility":"public"}],
  "next_cursor": null
}
```

## Project follows

- `GET /rest/v1/project_follows?user_id=eq.<caller-uuid>&select=project_id,created_at`
  lists the caller's followed projects. Other members' follows are not public.
- `POST /rest/v1/project_follows` accepts `{ "project_id": UUID }` for the
  authenticated caller. `user_id` is derived from the JWT, never accepted from
  the client. Repeating a follow is idempotent and returns the existing
  relationship; it does not create a duplicate.
- `DELETE /rest/v1/project_follows?project_id=eq.<uuid>&user_id=eq.<caller-uuid>`
  removes the caller's follow. Repeating the delete succeeds as an empty
  result.

## Founder controls

Every Founder operation checks the authenticated `founder` database role. The
Founder APK only exposes these controls after the server confirms the role.
Changing the build flavor, local storage, or request body cannot grant access.
Founder writes create a redacted, append-only `admin_audit_log` record in the
same database transaction as the change. Audit summaries must not contain
invitation links, post/comment bodies, report evidence, or private messages.

The Founder client uses the following RPCs:

- `founder_list_members(p_cursor, p_limit)` returns member IDs, handles,
  display names, account state and platform roles. It does not return Auth
  email addresses. `founder_list_audit(p_cursor, p_limit)` returns paginated
  audit records.
- `founder_set_member_state(p_user_id, p_state)` accepts `active`,
  `suspended`, or `closed`. `founder_set_platform_role(p_user_id, p_role,
  p_enabled)` accepts only `administrator` or `moderator`; the Founder role
  itself remains operator-managed.
- `founder_upsert_project(p_project_id, p_slug, p_owner_category, p_title,
  p_summary, p_stage, p_tags, p_visibility)` creates or updates catalogue
  entries. `founder_set_project_membership(p_project_id, p_user_id, p_role)`
  accepts `owner`, `maintainer`, `contributor`, `tester`, or `null` to remove
  a membership.
- `founder_upsert_topic(p_topic_id, p_slug, p_title, p_description,
  p_visibility)` creates or updates community topics.
  `founder_list_topics(p_cursor, p_limit)` pages through public, member-only,
  and private topics visible to the Founder.
  `founder_set_topic_membership(p_topic_id, p_user_id, p_enabled)` grants or
  removes membership for an active account.
- `founder_set_app_setting(p_key, p_value)` allows only `maintenance_notice`
  (string up to 240 characters) and `feature_flags` (an object of at most 64
  boolean values with bounded key names).
- `founder_list_content(p_state, p_cursor, p_limit)` returns bounded post and
  comment details for review, filtered to `visible`, `hidden`, `removed`, or
  `all`. `founder_set_content_moderation(p_content_type, p_content_id,
  p_state)` accepts `post` or `comment` and `visible`, `hidden`, or `removed`.
  General private messages are not included in this interface.

All list calls cap a page at 50 and return an opaque `next_cursor`. Founder
operations return JSON describing the accepted change; clients reload the
corresponding page to read back current server state. The audit table is
read-only to the Founder and cannot be updated or deleted by API roles.

## Status and error mapping

| HTTP status | Meaning | Client behavior |
|---|---|---|
| 400 | Invalid field, filter, cursor or request body | Show field-level validation where safe; do not retry unchanged |
| 401 | Missing, invalid or expired session | Refresh once if possible; otherwise clear session and request sign-in |
| 403 | Authenticated caller lacks permission | Show access denied; do not retry |
| 404 | Resource absent or intentionally hidden by visibility policy | Show unavailable without disclosing hidden data |
| 409 | Unique or state conflict not handled idempotently | Refresh the affected state |
| 429 | Rate limit | Respect `Retry-After` when provided and use bounded backoff |
| 5xx / network error | Temporary service failure | Preserve user input and offer bounded retry |

Never include database text, stack traces, secrets, invite tokens or private
content in user-facing errors or logs.

## Compatibility policy

Additive nullable response fields, new optional filters and new enum values
with a documented safe fallback may be introduced compatibly. Removing or
renaming fields, changing types or meanings, changing auth requirements,
changing ordering/cursor semantics, or tightening access in a way that breaks
supported clients is breaking. Before a breaking change, publish a new
contract version and migration plan, update Android compatibility, and keep
the old contract available through its support window. Contract and database
migration changes must land together; do not change the Android client against
an undocumented API.
