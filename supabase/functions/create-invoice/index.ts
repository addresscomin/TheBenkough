import { createClient } from "npm:@supabase/supabase-js@2";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return Response.json({ error: "POST required" }, { status: 405 });
  try {
    const authHeader = req.headers.get("Authorization") ?? "";
    const bearer = authHeader.replace(/^Bearer\s+/i, "");
    if (!bearer) return Response.json({ error: "Sign-in required." }, { status: 401 });
    const url = Deno.env.get("SUPABASE_URL");
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const publishableRaw = Deno.env.get("SUPABASE_PUBLISHABLE_KEYS");
    const publishableKeys = publishableRaw ? JSON.parse(publishableRaw) : {};
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? publishableKeys.default ?? Object.values(publishableKeys)[0];
    if (!url || !serviceRole || !anonKey) throw new Error("Supabase server configuration is incomplete.");
    const userClient = createClient(url, anonKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: { user }, error: authError } = await userClient.auth.getUser(bearer);
    if (authError || !user || user.is_anonymous) return Response.json({ error: "A verified account is required." }, { status: 403 });
    const payload = await req.json();
    const clientName = String(payload.client_name ?? "").trim().slice(0, 160);
    const description = String(payload.description ?? "").trim().slice(0, 240);
    const quantity = Number(payload.quantity);
    const unitPrice = Number(payload.unit_price_rupees);
    const taxPercent = Number(payload.tax_percent ?? 0);
    if (!clientName || !description || !Number.isFinite(quantity) || quantity <= 0 || quantity > 100000 ||
        !Number.isFinite(unitPrice) || unitPrice < 0 || unitPrice > 100000000 ||
        !Number.isFinite(taxPercent) || taxPercent < 0 || taxPercent > 100) {
      return Response.json({ error: "Enter valid client, item, quantity, price and tax values." }, { status: 400 });
    }
    const admin = createClient(url, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });
    const { data: workspace, error: workspaceError } = await admin.from("workspaces")
      .select("id").eq("owner_id", user.id).eq("workspace_type", "personal").limit(1).maybeSingle();
    if (workspaceError) throw workspaceError;
    if (!workspace) return Response.json({ error: "Personal workspace not found." }, { status: 403 });
    const subtotalMinor = Math.round(quantity * unitPrice * 100);
    const taxMinor = Math.round(subtotalMinor * taxPercent / 100);
    const totalMinor = subtotalMinor + taxMinor;
    const now = new Date();
    const date = now.toISOString().slice(0, 10);
    const invoiceNumber = `BK-${date.replaceAll("-", "").slice(2)}-${crypto.randomUUID().slice(0, 4).toUpperCase()}`;
    const { data: invoice, error: invoiceError } = await admin.from("invoices").insert({
      workspace_id: workspace.id, invoice_number: invoiceNumber, client_name: clientName, currency: "INR",
      subtotal_minor: subtotalMinor, tax_minor: taxMinor, total_minor: totalMinor, status: "issued",
      issue_date: date, created_by: user.id,
    }).select("id, invoice_number, client_name, subtotal_minor, tax_minor, total_minor, status, issue_date, due_date, created_at").single();
    if (invoiceError) throw invoiceError;
    const { error: itemError } = await admin.from("invoice_items").insert({
      invoice_id: invoice.id, description, quantity, unit_price_minor: Math.round(unitPrice * 100), line_total_minor: subtotalMinor,
    });
    if (itemError) {
      await admin.from("invoices").delete().eq("id", invoice.id).eq("workspace_id", workspace.id);
      throw itemError;
    }
    return Response.json({ ok: true, invoice }, { status: 201 });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unexpected server error.";
    return Response.json({ error: message.slice(0, 200) }, { status: 500 });
  }
});
