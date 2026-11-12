-- Fix row-to-membership correlation in workspace RLS policies.
-- Applied to the live project with execute_sql; this migration keeps future environments aligned.

DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY['assets','clients','content_items','leads','projects','tasks'] LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', t || '_select_member', t);
    EXECUTE format(
      'CREATE POLICY %I ON public.%I FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = %I.workspace_id AND wm.user_id = (SELECT auth.uid())))',
      t || '_select_member', t, t
    );

    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', t || '_insert_editor', t);
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', t || '_insert_member', t);
    IF t = 'projects' OR t = 'content_items' OR t = 'tasks' THEN
      EXECUTE format(
        'CREATE POLICY %I ON public.%I FOR INSERT TO authenticated WITH CHECK (created_by = (SELECT auth.uid()) AND EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = %I.workspace_id AND wm.user_id = (SELECT auth.uid()) AND wm.role IN (''owner'',''admin'',''editor'')))',
        t || CASE WHEN t='projects' THEN '_insert_member' ELSE '_insert_editor' END, t, t
      );
    ELSE
      EXECUTE format(
        'CREATE POLICY %I ON public.%I FOR INSERT TO authenticated WITH CHECK (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = %I.workspace_id AND wm.user_id = (SELECT auth.uid()) AND wm.role IN (''owner'',''admin'',''editor'')))',
        t || '_insert_editor', t, t
      );
    END IF;

    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', t || '_update_editor', t);
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', t || '_update_member', t);
    EXECUTE format(
      'CREATE POLICY %I ON public.%I FOR UPDATE TO authenticated USING (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = %I.workspace_id AND wm.user_id = (SELECT auth.uid()) AND wm.role IN (''owner'',''admin'',''editor''))) WITH CHECK (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = %I.workspace_id AND wm.user_id = (SELECT auth.uid()) AND wm.role IN (''owner'',''admin'',''editor'')))',
      t || CASE WHEN t='projects' THEN '_update_member' ELSE '_update_editor' END, t, t, t
    );

    EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', t || '_delete_admin', t);
    EXECUTE format(
      'CREATE POLICY %I ON public.%I FOR DELETE TO authenticated USING (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = %I.workspace_id AND wm.user_id = (SELECT auth.uid()) AND wm.role IN (''owner'',''admin'')))',
      t || '_delete_admin', t, t
    );
  END LOOP;
END $$;

DROP POLICY IF EXISTS invoices_select_member ON public.invoices;
CREATE POLICY invoices_select_member ON public.invoices FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = invoices.workspace_id AND wm.user_id = (SELECT auth.uid())));

DROP POLICY IF EXISTS activity_events_select_member ON public.activity_events;
CREATE POLICY activity_events_select_member ON public.activity_events FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.workspace_members wm WHERE wm.workspace_id = activity_events.workspace_id AND wm.user_id = (SELECT auth.uid())));
