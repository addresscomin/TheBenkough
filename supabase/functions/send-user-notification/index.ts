import { createClient } from "npm:@supabase/supabase-js@2";
import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

async function googleAccessToken(account: ServiceAccount): Promise<string> {
  const key = await importPKCS8(account.private_key, "RS256");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(account.client_email)
    .setSubject(account.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const result = await response.json();
  if (!response.ok || !result.access_token) {
    throw new Error("Google OAuth token exchange failed.");
  }
  return result.access_token as string;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return Response.json({ error: "POST required" }, { status: 405 });
  }

  try {
    const authHeader = req.headers.get("Authorization") ?? "";
    const bearer = authHeader.replace(/^Bearer\s+/i, "");
    if (!bearer) return Response.json({ error: "Sign-in required" }, { status: 401 });

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
    if (!supabaseUrl || !serviceRole || !anonKey) {
      throw new Error("Supabase server environment is incomplete.");
    }

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: { user }, error: userError } = await userClient.auth.getUser(bearer);
    if (userError || !user || user.is_anonymous) {
      return Response.json({ error: "A verified, non-anonymous account is required." }, { status: 403 });
    }

    const payload = await req.json();
    const title = String(payload.title ?? "").trim().slice(0, 100);
    const body = String(payload.body ?? "").trim().slice(0, 1500);
    if (!title || !body) {
      return Response.json({ error: "Title and body are required." }, { status: 400 });
    }

    const admin = createClient(supabaseUrl, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: notification, error: insertError } = await admin
      .from("notifications")
      .insert({ user_id: user.id, title, body, data: { source: "benkough_app" } })
      .select("id")
      .single();
    if (insertError) throw insertError;

    const serviceAccountRaw = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
    if (!serviceAccountRaw) {
      return Response.json({
        ok: true,
        notification_id: notification.id,
        push_status: "stored_only",
        detail: "FCM_SERVICE_ACCOUNT_JSON is not configured yet.",
      }, { status: 202 });
    }

    const account = JSON.parse(serviceAccountRaw) as ServiceAccount;
    if (!account.project_id || !account.client_email || !account.private_key) {
      throw new Error("FCM service account JSON is incomplete.");
    }

    const { data: devices, error: tokenError } = await admin
      .from("device_tokens")
      .select("fcm_token")
      .eq("user_id", user.id)
      .limit(20);
    if (tokenError) throw tokenError;
    if (!devices?.length) {
      return Response.json({
        ok: true,
        notification_id: notification.id,
        push_status: "no_registered_devices",
      }, { status: 202 });
    }

    const accessToken = await googleAccessToken(account);
    const results = await Promise.all(devices.map(async (device: { fcm_token: string }) => {
      const response = await fetch(
        `https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`,
        {
          method: "POST",
          headers: {
            authorization: `Bearer ${accessToken}`,
            "content-type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token: device.fcm_token,
              notification: { title, body },
              data: { notification_id: notification.id },
              android: { priority: "HIGH" },
            },
          }),
        },
      );
      if (!response.ok) {
        const errorText = await response.text();
        if (errorText.includes("UNREGISTERED") || errorText.includes("registration-token-not-registered")) {
          await admin.from("device_tokens").delete().eq("fcm_token", device.fcm_token);
        }
        return false;
      }
      return true;
    }));

    const sent = results.filter(Boolean).length;
    return Response.json({
      ok: true,
      notification_id: notification.id,
      push_status: sent > 0 ? "sent" : "failed",
      sent,
      failed: results.length - sent,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unexpected server error";
    return Response.json({ error: message.slice(0, 220) }, { status: 500 });
  }
});
