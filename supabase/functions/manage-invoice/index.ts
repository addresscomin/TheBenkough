import { createClient } from "npm:@supabase/supabase-js@2";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return Response.json({ error: "POST required" }, { status: 405 });
  try {
    const authHeader = req.headers.get("Authorization") ?? "";
    const bearer = authHeader.replace(/^Bearer\\s+/i, "");
    if (!bearer) return Response.json({ error: "Sign-in required." }, { status: 401 });
    const url = Deno.env.get("SUPABASE_URL");
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const publishableRaw = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
    const keys = publishableRaw ? JSON.parse(publishableRaw) : {};
    const publicKey = Deno.env.get("SUPABASE_ANON_KEY") ?? keys.default ?? Object.values(keys)[0];
    if (!url || !serviceRole || !publicKey) throw new Error("Supabase server configuration is incomplete.");
    const userClient = createClient(url, publicKey, { global: { headers: { Authorization: authHeader } }, auth: { persistSession: false, autoRefreshToken: false } });
    const { data: { user }, error: authError } = await userClient.auth.getUser(bearer);
    if (authError || !user || user.is_anonymous) return Response.json({ error: "A verified account is required." }, { status: 403 });
    const payload = await req.json();
    const invoiceId = String(payload.invoice_id ?? "");
    const action = String(payload.action ?? "");
    if (!/^[0-9a-f-]{36}$/i.test(invoiceId) || !["mark_paid", "delete"].includes(action)) {
      return Response.json({ error: "Invalid invoice action." }, { status: 400 });
    }
    const admin = createClient(url, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });
    const { data: workspace, error: workspaceError } = await admin.from("workspaces")
      .select("id").eq("owner_id", user.id).eq("workspace_type", "personal").limit(1).maybeSingle();
    if (workspaceError) throw workspaceError;
    if (!workspace) return Response.json({ error: "Personal workspace not found." }, { status: 403 });
    if (action === "mark_paid") {
      const { data, error } = await admin.from("invoices").update({ status: "paid" })
        .eq("id", invoiceId).eq("workspace_id", workspace.id)
        .neq("status", "cancelled").select("id").maybeSingle();
      if (error) throw error;
      if (!data) return Response.json({ error: "Invoice not found or cannot be marked paid." }, { status: 404 });
      return Response.json({ ok: true, action, invoice_id: invoiceId });
    }
    const { data, error } = await admin.from("invoices").delete()
      .eq("id", invoiceId).eq("workspace_id", workspace.id).select("id").maybeSingle();
    if (error) throw error;
    if (!data) return Response.json({ error: "Invoice not found." }, { status: 404 });
    return Response.json({ ok: true, action, invoice_id: invoiceId });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unexpected server error.";
    return Response.json({ error: message.slice(0, 200) }, { status: 500 });
  }
});
