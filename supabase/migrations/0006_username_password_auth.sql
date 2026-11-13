-- Username-based password accounts for THE BENKOUGH.
-- Supabase Auth still verifies passwords and issues the session; the app maps
-- a normalized username to a reserved internal Auth email identifier.
-- No email/phone ownership is claimed by this identifier.

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS username text;

CREATE UNIQUE INDEX IF NOT EXISTS profiles_username_lower_unique
  ON public.profiles (lower(username))
  WHERE username IS NOT NULL;

ALTER TABLE public.profiles
  DROP CONSTRAINT IF EXISTS profiles_username_format_check;

ALTER TABLE public.profiles
  ADD CONSTRAINT profiles_username_format_check
  CHECK (username IS NULL OR username ~ '^[a-z0-9_]{3,20}$');

CREATE OR REPLACE FUNCTION private.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  personal_workspace_id uuid;
  new_username text;
BEGIN
  new_username := lower(coalesce(
    nullif(new.raw_user_meta_data ->> 'username', ''),
    'user_' || left(replace(new.id::text, '-', ''), 12)
  ));

  IF new_username !~ '^[a-z0-9_]{3,20}$' THEN
    RAISE EXCEPTION 'A valid username is required';
  END IF;

  INSERT INTO public.profiles (id, display_name, username)
  VALUES (
    new.id,
    coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), new_username),
    new_username
  );

  INSERT INTO public.user_preferences (user_id) VALUES (new.id);

  INSERT INTO public.workspaces (name, workspace_type, owner_id)
  VALUES (new_username || '''s Workspace', 'personal', new.id)
  RETURNING id INTO personal_workspace_id;

  INSERT INTO public.workspace_members (workspace_id, user_id, role)
  VALUES (personal_workspace_id, new.id, 'owner');

  RETURN new;
END;
$$;

REVOKE ALL ON FUNCTION private.handle_new_user() FROM public, anon, authenticated;
