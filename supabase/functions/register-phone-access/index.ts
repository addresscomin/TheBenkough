// Retired legacy endpoint. Do not restore custom phone-confirmation bypass.
Deno.serve(async (_req: Request) => {
  return Response.json(
    {
      error: "This legacy endpoint is disabled. It created phone-confirmed accounts without verifying phone ownership. Use Supabase Auth SMS OTP instead."
    },
    { status: 410, headers: { "Cache-Control": "no-store" } }
  );
});
