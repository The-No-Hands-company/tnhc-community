import { createInviteMemberHandler } from "../../functions/invite-member/index.ts";

const ADMIN_ID = "10000000-0000-0000-0000-000000000001";

function request(token = "test-user-token", email = "New.Member@example.test"): Request {
  return new Request("http://local/functions/v1/invite-member", {
    method: "POST",
    headers: {
      ...(token ? { authorization: `Bearer ${token}` } : {}),
      "content-type": "application/json",
    },
    body: JSON.stringify({ email }),
  });
}

function harness(options: { admin?: boolean; founder?: boolean; conflict?: boolean; inviteFails?: boolean; redirect?: string | null } = {}) {
  const calls: Array<{ url: string; init?: RequestInit }> = [];
  const fetcher: typeof fetch = async (input, init) => {
    const url = String(input);
    calls.push({ url, init });
    if (url.endsWith("/auth/v1/user")) {
      return Response.json({ id: ADMIN_ID, email: "admin@example.test" });
    }
    if (url.includes("/rest/v1/platform_roles")) {
      return Response.json(options.admin ? [{ role: "administrator" }] : options.founder ? [{ role: "founder" }] : []);
    }
    if (url.includes("/rest/v1/invitations?") && init?.method === "DELETE") {
      return new Response(null, { status: 204 });
    }
    if (url.includes("/rest/v1/invitations?") && init?.method === "POST") {
      if (options.conflict) return Response.json({ code: "23505" }, { status: 409 });
      return Response.json([{ id: "20000000-0000-0000-0000-000000000001" }], { status: 201 });
    }
    if (url.endsWith("/rest/v1/admin_audit_log") && init?.method === "POST") {
      return new Response(null, { status: 201 });
    }
    if (new URL(url).pathname === "/auth/v1/invite") {
      return options.inviteFails ? Response.json({ message: "private provider detail" }, { status: 500 }) : Response.json({ id: "ignored-auth-user" }, { status: 200 });
    }
    if (url.includes("/rest/v1/invitations?id=eq.")) return new Response(null, { status: 204 });
    return new Response(null, { status: 404 });
  };
  const handler = createInviteMemberHandler({
    env: (name) => ({
      SUPABASE_URL: "http://127.0.0.1:54321",
      SUPABASE_ANON_KEY: "public-test-key",
      SUPABASE_SERVICE_ROLE_KEY: "server-only-test-key",
      INVITE_REDIRECT_URL: options.redirect === null ? undefined : options.redirect ?? "tnhccommunity://invite",
    }[name]),
    fetcher,
    now: () => new Date("2026-10-03T12:00:00.000Z"),
  });
  return { handler, calls };
}

Deno.test("anonymous caller cannot issue an invitation", async () => {
  const { handler, calls } = harness({ admin: true });
  const response = await handler(request(""));
  if (response.status !== 401 || calls.length !== 0) throw new Error("anonymous request was not rejected before backend calls");
});

Deno.test("ordinary member cannot issue an invitation", async () => {
  const { handler, calls } = harness();
  const response = await handler(request());
  if (response.status !== 403) throw new Error(`expected 403, got ${response.status}`);
  if (calls.some(({ url }) => url.includes("/invitations") || new URL(url).pathname === "/auth/v1/invite")) throw new Error("unauthorized invite was attempted");
});

Deno.test("administrator invitation stores a normalized address and returns no Auth token", async () => {
  const { handler, calls } = harness({ admin: true });
  const response = await handler(request());
  const body = await response.json();
  if (response.status !== 200 || body.status !== "sent" || body.invitation_id !== "20000000-0000-0000-0000-000000000001") throw new Error("administrator invite response is incorrect");
  const insert = calls.find(({ url, init }) => url.includes("/rest/v1/invitations?") && init?.method === "POST");
  const saved = JSON.parse(String(insert?.init?.body));
  if (saved.normalized_email !== "new.member@example.test" || saved.invited_by !== ADMIN_ID) throw new Error("invitation fields were not normalized or attributed");
  const authInvite = calls.find(({ url }) => new URL(url).pathname === "/auth/v1/invite");
  const payload = JSON.parse(String(authInvite?.init?.body));
  if (payload.email !== saved.normalized_email || new URL(authInvite!.url).searchParams.get("redirect_to") !== "tnhccommunity://invite") throw new Error("Auth invite request is incorrect");
  if (JSON.stringify(body).includes("token") || JSON.stringify(body).includes("service-only")) throw new Error("response leaked credential material");
});

Deno.test("Founder can issue an invitation and records a redacted audit event", async () => {
  const { handler, calls } = harness({ founder: true });
  const response = await handler(request());
  const rolesCall = calls.find(({ url }) => url.includes("/rest/v1/platform_roles"));
  const auditCall = calls.find(({ url, init }) => url.endsWith("/rest/v1/admin_audit_log") && init?.method === "POST");
  if (response.status !== 200 || !rolesCall || new URL(rolesCall.url).searchParams.get("role") !== "in.(administrator,founder)") {
    throw new Error("Founder invitation authorization was not checked with the server role");
  }
  if (!auditCall) throw new Error("Founder invitation audit request is missing");
  const audit = JSON.parse(String(auditCall.init?.body));
  if (audit.actor_id !== ADMIN_ID || audit.action !== "invitation_requested" || audit.target_id !== "20000000-0000-0000-0000-000000000001") {
    throw new Error("Founder invitation action was not recorded against its actor and invitation");
  }
  if (JSON.stringify(audit).includes("new.member@example.test")) throw new Error("audit record must not contain the invited address");
});

Deno.test("duplicate pending address returns a conflict", async () => {
  const { handler } = harness({ admin: true, conflict: true });
  const response = await handler(request());
  const body = await response.json();
  if (response.status !== 409 || body.code !== "INVITATION_PENDING") throw new Error("duplicate invitation was not mapped to a conflict");
});

Deno.test("expired pending invitation is cleared before a new invite", async () => {
  const { handler, calls } = harness({ admin: true });
  await handler(request());
  const cleanup = calls.find(({ url, init }) => url.includes("/rest/v1/invitations?") && init?.method === "DELETE");
  const cleanupUrl = new URL(cleanup?.url ?? "http://invalid");
  if (!cleanupUrl.searchParams.get("expires_at")?.startsWith("lt.")) throw new Error("expired pending invitations were not cleared");
  if (cleanupUrl.searchParams.get("accepted_at") !== "is.null") throw new Error("accepted invitations must not be cleared");
});

Deno.test("Auth email failure removes the pending row and hides provider details", async () => {
  const { handler, calls } = harness({ admin: true, inviteFails: true });
  const response = await handler(request());
  const text = await response.text();
  if (response.status !== 502 || text.includes("private provider detail")) throw new Error("provider error was not safely mapped");
  if (!calls.some(({ url, init }) => url.includes("/rest/v1/invitations?id=eq.") && init?.method === "DELETE")) throw new Error("failed invite did not clean up its pending row");
});

Deno.test("Android invitation redirect is the default without extra environment configuration", async () => {
  const { handler, calls } = harness({ admin: true, redirect: null });
  const response = await handler(request());
  const invite = calls.find(({ url }) => new URL(url).pathname === "/auth/v1/invite");
  if (response.status !== 200 || !invite || new URL(invite.url).searchParams.get("redirect_to") !== "tnhccommunity://invite") {
    throw new Error("unconfigured invitation did not return to Android");
  }
});

Deno.test("configured invitation redirect is encoded as a query parameter", async () => {
  const redirect = "https://community.example.test/accept?source=invite&lang=en";
  const { handler, calls } = harness({ admin: true, redirect });
  const response = await handler(request());
  const invite = calls.find(({ url }) => new URL(url).pathname === "/auth/v1/invite");
  if (response.status !== 200 || !invite || new URL(invite.url).searchParams.get("redirect_to") !== redirect) {
    throw new Error("custom invitation redirect was not preserved");
  }
});
