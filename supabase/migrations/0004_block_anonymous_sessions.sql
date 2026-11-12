-- Prevent Supabase anonymous-auth sessions from accessing private workspace data.
-- Anonymous users carry the authenticated Postgres role, so TO authenticated
-- alone is not sufficient. These restrictive policies are ANDed with the
-- existing workspace-membership policies and preserve normal signed-in users.
DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'activity_events','assets','clients','content_items','device_tokens',
    'invoice_items','invoices','leads','notifications','profiles',
    'projects','tasks','templates','user_preferences','workspace_members','workspaces'
  ] LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', 'benkough_block_anonymous_sessions', t);
    EXECUTE format(
      'CREATE POLICY %I ON public.%I AS RESTRICTIVE FOR ALL TO authenticated USING ((SELECT auth.jwt() ->> ''is_anonymous'') IS DISTINCT FROM ''true'') WITH CHECK ((SELECT auth.jwt() ->> ''is_anonymous'') IS DISTINCT FROM ''true'')',
      'benkough_block_anonymous_sessions', t
    );
  END LOOP;
END $$;

DROP POLICY IF EXISTS benkough_block_anonymous_sessions ON storage.objects;
CREATE POLICY benkough_block_anonymous_sessions ON storage.objects
AS RESTRICTIVE FOR ALL TO authenticated
USING ((SELECT auth.jwt() ->> 'is_anonymous') IS DISTINCT FROM 'true')
WITH CHECK ((SELECT auth.jwt() ->> 'is_anonymous') IS DISTINCT FROM 'true');
