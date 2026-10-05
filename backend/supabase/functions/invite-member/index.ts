type Environment = (name: string) => string | undefined;
type Fetcher = typeof fetch;

interface Dependencies {
  env: Environment;
  fetcher: Fetcher;
  now: () => Date;
}

interface AuthUser {
  id: string;
  email?: string;
}

const json = (status: number, body: Record<string, unknown>): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });

const error = (status: number, code: string, message: string): Response =>
  json(status, { code, message, details: null, hint: null });

function parseBearer(request: Request): string | null {
  const value = request.headers.get("authorization");
  const match = value?.match(/^Bearer\s+([^\s]+)$/i);
  return match?.[1] ?? null;
}

function isEmail(value: unknown): value is string {
  return typeof value === "string" && value.length <= 254 &&
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function createInviteMemberHandler({ env, fetcher, now }: Dependencies) {
  return async (request: Request): Promise<Response> => {
    if (request.method !== "POST") {
      return error(405, "METHOD_NOT_ALLOWED", "Use POST for this operation.");
    }

    const bearer = parseBearer(request);
    if (!bearer) return error(401, "AUTH_REQUIRED", "Sign in with an administrator or Founder account.");

    let payload: unknown;
    try {
      payload = await request.json();
    } catch {
      return error(400, "INVALID_JSON", "The request body must be valid JSON.");
    }
    const candidate = (payload as { email?: unknown } | null)?.email;
    if (!isEmail(candidate)) return error(400, "INVALID_EMAIL", "Enter a valid email address.");

    const baseUrl = env("SUPABASE_URL")?.replace(/\/$/, "");
    const publishableKey = env("SUPABASE_ANON_KEY");
    const serviceKey = env("SUPABASE_SERVICE_ROLE_KEY");
    if (!baseUrl || !publishableKey || !serviceKey) {
      return error(503, "INVITATION_UNAVAILABLE", "Invitations are not configured.");
    }

    let userResponse: Response;
    try {
      userResponse = await fetcher(`${baseUrl}/auth/v1/user`, {
        headers: { apikey: publishableKey, authorization: `Bearer ${bearer}` },
      });
    } catch {
      return error(503, "AUTH_UNAVAILABLE", "Authentication could not be checked.");
    }
    if (userResponse.status === 401 || userResponse.status === 403) {
      return error(401, "AUTH_REQUIRED", "Sign in with an administrator or Founder account.");
    }
    if (!userResponse.ok) return error(503, "AUTH_UNAVAILABLE", "Authentication could not be checked.");

    let user: AuthUser;
    try {
      user = await userResponse.json();
    } catch {
      return error(503, "AUTH_UNAVAILABLE", "Authentication could not be checked.");
    }
    if (typeof user.id !== "string" || !/^[0-9a-f-]{36}$/i.test(user.id)) {
      return error(401, "AUTH_REQUIRED", "Sign in with an administrator or Founder account.");
    }

    let roleResponse: Response;
    try {
      const roleParams = new URLSearchParams({
        user_id: `eq.${user.id}`,
        role: "in.(administrator,founder)",
        select: "role",
        limit: "1",
      });
      roleResponse = await fetcher(
        `${baseUrl}/rest/v1/platform_roles?${roleParams}`,
        { headers: { apikey: publishableKey, authorization: `Bearer ${bearer}` } },
      );
    } catch {
      return error(503, "AUTHZ_UNAVAILABLE", "Administrator access could not be checked.");
    }
    if (!roleResponse.ok) return error(503, "AUTHZ_UNAVAILABLE", "Administrator access could not be checked.");
    const roles = await roleResponse.json().catch(() => null);
    if (!Array.isArray(roles) || !roles.some((item) => item?.role === "administrator" || item?.role === "founder")) {
      return error(403, "ADMIN_REQUIRED", "Only an administrator or Founder can invite members.");
    }

    const email = candidate.trim().toLowerCase();
    const invitation = {
      normalized_email: email,
      invited_by: user.id,
      expires_at: new Date(now().getTime() + 7 * 24 * 60 * 60 * 1000).toISOString(),
    };

    const expiredParams = new URLSearchParams({
      normalized_email: `eq.${email}`,
      accepted_at: "is.null",
      expires_at: `lt.${now().toISOString()}`,
    });
    try {
      const expiredCleanup = await fetcher(`${baseUrl}/rest/v1/invitations?${expiredParams}`, {
        method: "DELETE",
        headers: { apikey: serviceKey, authorization: `Bearer ${serviceKey}` },
      });
      if (!expiredCleanup.ok) return error(503, "INVITATION_UNAVAILABLE", "The invitation could not be created.");
    } catch {
      return error(503, "INVITATION_UNAVAILABLE", "The invitation could not be created.");
    }

    let invitationResponse: Response;
    try {
      invitationResponse = await fetcher(`${baseUrl}/rest/v1/invitations?select=id`, {
        method: "POST",
        headers: {
          apikey: serviceKey,
          authorization: `Bearer ${serviceKey}`,
          "content-type": "application/json",
          prefer: "return=representation",
        },
        body: JSON.stringify(invitation),
      });
    } catch {
      return error(503, "INVITATION_UNAVAILABLE", "The invitation could not be created.");
    }
    if (invitationResponse.status === 409) {
      return error(409, "INVITATION_PENDING", "A pending invitation already exists for this address.");
    }
    if (!invitationResponse.ok) {
      return error(503, "INVITATION_UNAVAILABLE", "The invitation could not be created.");
    }

    const rows = await invitationResponse.json().catch(() => null);
    const invitationId = Array.isArray(rows) ? rows[0]?.id : null;
    if (typeof invitationId !== "string") {
      return error(503, "INVITATION_UNAVAILABLE", "The invitation could not be created.");
    }

    let auditResponse: Response;
    try {
      auditResponse = await fetcher(`${baseUrl}/rest/v1/admin_audit_log`, {
        method: "POST",
        headers: {
          apikey: serviceKey,
          authorization: `Bearer ${serviceKey}`,
          "content-type": "application/json",
          prefer: "return=minimal",
        },
        body: JSON.stringify({
          actor_id: user.id,
          action: "invitation_requested",
          target_type: "invitation",
          target_id: invitationId,
          summary: "Invitation requested.",
        }),
      });
    } catch {
      auditResponse = new Response(null, { status: 503 });
    }
    if (!auditResponse.ok) {
      try {
        await fetcher(`${baseUrl}/rest/v1/invitations?id=eq.${encodeURIComponent(invitationId)}`, {
          method: "DELETE",
          headers: { apikey: serviceKey, authorization: `Bearer ${serviceKey}` },
        });
      } catch {
        // Expiry bounds the unused record if audit storage is temporarily unavailable.
      }
      return error(503, "INVITATION_UNAVAILABLE", "The invitation could not be created.");
    }

    const redirectTo = env("INVITE_REDIRECT_URL") || "tnhccommunity://invite";
    const inviteUrl = new URL(`${baseUrl}/auth/v1/invite`);
    inviteUrl.searchParams.set("redirect_to", redirectTo);
    let authInviteResponse: Response;
    try {
      authInviteResponse = await fetcher(inviteUrl.toString(), {
        method: "POST",
        headers: {
          apikey: serviceKey,
          authorization: `Bearer ${serviceKey}`,
          "content-type": "application/json",
        },
        body: JSON.stringify({ email }),
      });
    } catch {
      authInviteResponse = new Response(null, { status: 503 });
    }

    if (!authInviteResponse.ok) {
      try {
        await fetcher(`${baseUrl}/rest/v1/invitations?id=eq.${encodeURIComponent(invitationId)}`, {
          method: "DELETE",
          headers: { apikey: serviceKey, authorization: `Bearer ${serviceKey}` },
        });
      } catch {
        // Expiry bounds the unused record if cleanup is temporarily unavailable.
      }
      return error(502, "EMAIL_DELIVERY_FAILED", "The invitation email could not be sent.");
    }

    return json(200, { invitation_id: invitationId, status: "sent" });
  };
}

if (import.meta.main) {
  Deno.serve(createInviteMemberHandler({
    env: (name) => Deno.env.get(name),
    fetcher: fetch,
    now: () => new Date(),
  }));
}
