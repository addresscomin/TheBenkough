-- THE BENKOUGH initial schema
-- Apply only to a dedicated THE BENKOUGH Supabase project.
-- This migration intentionally does not touch any existing client database.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  avatar_path text,
  locale text not null default 'en-IN',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.workspaces (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(name) between 1 and 80),
  workspace_type text not null check (workspace_type in ('personal','business')),
  owner_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.workspace_members (
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('owner','admin','editor','viewer')),
  joined_at timestamptz not null default now(),
  primary key (workspace_id, user_id)
);

create table if not exists public.projects (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 140),
  description text not null default '',
  status text not null default 'active' check (status in ('draft','active','on_hold','completed','archived')),
  due_at timestamptz,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.tasks (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  project_id uuid references public.projects(id) on delete set null,
  title text not null check (char_length(title) between 1 and 180),
  description text not null default '',
  status text not null default 'todo' check (status in ('todo','in_progress','blocked','done')),
  priority text not null default 'normal' check (priority in ('low','normal','high','urgent')),
  due_at timestamptz,
  assigned_to uuid references auth.users(id) on delete set null,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.clients (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 160),
  email text,
  phone text,
  company text,
  notes text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.leads (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  name text not null,
  source text,
  stage text not null default 'new' check (stage in ('new','contacted','qualified','won','lost')),
  notes text not null default '',
  client_id uuid references public.clients(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.content_items (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  project_id uuid references public.projects(id) on delete set null,
  content_type text not null check (content_type in ('script','caption','storyboard','idea','calendar_item')),
  title text not null default 'Untitled',
  body text not null default '',
  status text not null default 'draft' check (status in ('draft','planned','in_progress','published','archived')),
  scheduled_at timestamptz,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.assets (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  project_id uuid references public.projects(id) on delete set null,
  storage_path text not null,
  original_name text not null,
  mime_type text not null,
  size_bytes bigint not null check (size_bytes >= 0),
  uploaded_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  unique (workspace_id, storage_path)
);

create table if not exists public.invoices (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  client_id uuid references public.clients(id) on delete set null,
  invoice_number text not null,
  currency char(3) not null default 'INR',
  subtotal_minor bigint not null default 0 check (subtotal_minor >= 0),
  tax_minor bigint not null default 0 check (tax_minor >= 0),
  total_minor bigint not null default 0 check (total_minor >= 0),
  status text not null default 'draft' check (status in ('draft','issued','paid','overdue','cancelled')),
  issue_date date,
  due_date date,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (workspace_id, invoice_number)
);

create table if not exists public.invoice_items (
  id uuid primary key default gen_random_uuid(),
  invoice_id uuid not null references public.invoices(id) on delete cascade,
  description text not null,
  quantity numeric(12,3) not null check (quantity > 0),
  unit_price_minor bigint not null check (unit_price_minor >= 0),
  line_total_minor bigint not null check (line_total_minor >= 0)
);

create table if not exists public.activity_events (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  actor_id uuid references auth.users(id) on delete set null,
  event_type text not null,
  entity_type text not null,
  entity_id uuid,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text not null default '',
  data jsonb not null default '{}'::jsonb,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null unique,
  platform text not null default 'android',
  updated_at timestamptz not null default now()
);

create table if not exists public.user_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  theme text not null default 'system' check (theme in ('system','dark','light')),
  language text not null default 'en',
  notification_settings jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

create table if not exists public.templates (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  category text not null,
  preview_path text,
  payload jsonb not null default '{}'::jsonb,
  is_published boolean not null default false,
  created_at timestamptz not null default now()
);

create index if not exists idx_workspace_members_user on public.workspace_members(user_id, workspace_id);
create index if not exists idx_projects_workspace on public.projects(workspace_id, updated_at desc);
create index if not exists idx_tasks_workspace_status on public.tasks(workspace_id, status, due_at);
create index if not exists idx_clients_workspace on public.clients(workspace_id, name);
create index if not exists idx_leads_workspace_stage on public.leads(workspace_id, stage);
create index if not exists idx_content_workspace_status on public.content_items(workspace_id, status, updated_at desc);
create index if not exists idx_assets_workspace on public.assets(workspace_id, created_at desc);
create index if not exists idx_invoices_workspace on public.invoices(workspace_id, created_at desc);
create index if not exists idx_activity_workspace on public.activity_events(workspace_id, created_at desc);
create index if not exists idx_notifications_user on public.notifications(user_id, created_at desc);

-- Profile rows are created automatically after Supabase Auth signup.
create schema if not exists private;
revoke all on schema private from public, anon, authenticated;

create or replace function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  personal_workspace_id uuid;
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', ''));
  insert into public.user_preferences (user_id) values (new.id);

  -- Every new account starts with a private personal workspace.
  insert into public.workspaces (name, workspace_type, owner_id)
  values (
    coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), 'My') || '''s Workspace',
    'personal',
    new.id
  )
  returning id into personal_workspace_id;

  insert into public.workspace_members (workspace_id, user_id, role)
  values (personal_workspace_id, new.id, 'owner');

  return new;
end;
$$;
revoke all on function private.handle_new_user() from public, anon, authenticated;
drop trigger if exists on_auth_user_created_benkough on auth.users;
create trigger on_auth_user_created_benkough
after insert on auth.users
for each row execute function private.handle_new_user();

-- RLS is enabled on every exposed application table.
alter table public.profiles enable row level security;
alter table public.workspaces enable row level security;
alter table public.workspace_members enable row level security;
alter table public.projects enable row level security;
alter table public.tasks enable row level security;
alter table public.clients enable row level security;
alter table public.leads enable row level security;
alter table public.content_items enable row level security;
alter table public.assets enable row level security;
alter table public.invoices enable row level security;
alter table public.invoice_items enable row level security;
alter table public.activity_events enable row level security;
alter table public.notifications enable row level security;
alter table public.device_tokens enable row level security;
alter table public.user_preferences enable row level security;
alter table public.templates enable row level security;

create policy "profiles_select_own" on public.profiles for select to authenticated using (id = (select auth.uid()));
create policy "profiles_insert_own" on public.profiles for insert to authenticated with check (id = (select auth.uid()));
create policy "profiles_update_own" on public.profiles for update to authenticated using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy "workspaces_select_member" on public.workspaces for select to authenticated
using (owner_id = (select auth.uid()) or exists (
  select 1 from public.workspace_members wm
  where wm.workspace_id = id and wm.user_id = (select auth.uid())
));
create policy "workspaces_insert_owner" on public.workspaces for insert to authenticated
with check (owner_id = (select auth.uid()));
create policy "workspaces_update_owner" on public.workspaces for update to authenticated
using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));
create policy "workspaces_delete_owner" on public.workspaces for delete to authenticated
using (owner_id = (select auth.uid()));

-- Members can read their own membership. Invitations and role changes are server-side operations.
create policy "workspace_members_select_self" on public.workspace_members for select to authenticated
using (user_id = (select auth.uid()));
create policy "workspace_members_insert_owner_self" on public.workspace_members for insert to authenticated
with check (
  user_id = (select auth.uid()) and role = 'owner'
  and exists (select 1 from public.workspaces w where w.id = workspace_id and w.owner_id = (select auth.uid()))
);
create policy "workspace_members_delete_self" on public.workspace_members for delete to authenticated
using (user_id = (select auth.uid()) and role <> 'owner');

-- Workspace-scoped tables: a user must have their own membership row.
create policy "projects_select_member" on public.projects for select to authenticated using (
  exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()))
);
create policy "projects_insert_member" on public.projects for insert to authenticated with check (
  created_by = (select auth.uid()) and exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in ('owner','admin','editor'))
);
create policy "projects_update_member" on public.projects for update to authenticated using (
  exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in ('owner','admin','editor'))
) with check (
  exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in ('owner','admin','editor'))
);
create policy "projects_delete_admin" on public.projects for delete to authenticated using (
  exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in ('owner','admin'))
);

-- Workspace-scoped operational tables. Viewers can read; editors and above can write.
do $$
declare t text;
begin
  foreach t in array array['tasks','clients','leads','content_items','assets'] loop
    execute format('create policy %I on public.%I for select to authenticated using (exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid())))', t || '_select_member', t);
    execute format('create policy %I on public.%I for insert to authenticated with check (exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in (''owner'',''admin'',''editor'')))', t || '_insert_editor', t);
    execute format('create policy %I on public.%I for update to authenticated using (exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in (''owner'',''admin'',''editor''))) with check (exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in (''owner'',''admin'',''editor'')))', t || '_update_editor', t);
    execute format('create policy %I on public.%I for delete to authenticated using (exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()) and wm.role in (''owner'',''admin'')))', t || '_delete_admin', t);
  end loop;
end $$;

-- Activity events are append-only and written by trusted server functions.
create policy "activity_events_select_member" on public.activity_events for select to authenticated using (
  exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()))
);

-- Invoices are read from the client. Creation, numbering and total calculations
-- must be performed by a trusted Edge Function; no direct client write policies.
create policy "invoices_select_member" on public.invoices for select to authenticated using (
  exists (select 1 from public.workspace_members wm where wm.workspace_id = workspace_id and wm.user_id = (select auth.uid()))
);

create policy "invoice_items_select_workspace" on public.invoice_items for select to authenticated using (
  exists (select 1 from public.invoices i join public.workspace_members wm on wm.workspace_id = i.workspace_id where i.id = invoice_id and wm.user_id = (select auth.uid()))
);
-- Invoice line-item writes are intentionally server-only until the trusted
-- invoice Edge Function validates and calculates all monetary values.

create policy "notifications_select_own" on public.notifications for select to authenticated using (user_id = (select auth.uid()));
create policy "notifications_update_own" on public.notifications for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "device_tokens_manage_own" on public.device_tokens for all to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "preferences_select_own" on public.user_preferences for select to authenticated using (user_id = (select auth.uid()));
create policy "preferences_insert_own" on public.user_preferences for insert to authenticated with check (user_id = (select auth.uid()));
create policy "preferences_update_own" on public.user_preferences for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "templates_read_published" on public.templates for select to anon, authenticated using (is_published = true);

-- Private buckets. Workspace asset object paths must begin with workspace UUID.
insert into storage.buckets (id, name, public) values
  ('avatars','avatars',true),
  ('workspace-assets','workspace-assets',false),
  ('exports','exports',false),
  ('template-previews','template-previews',true)
on conflict (id) do nothing;

create policy "avatar_upload_own_folder" on storage.objects for insert to authenticated
with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "avatar_update_own_folder" on storage.objects for update to authenticated
using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text)
with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "avatar_public_read" on storage.objects for select to anon, authenticated using (bucket_id = 'avatars');

create policy "workspace_asset_read_member" on storage.objects for select to authenticated using (
  bucket_id in ('workspace-assets','exports')
  and exists (select 1 from public.workspace_members wm where wm.workspace_id::text = (storage.foldername(name))[1] and wm.user_id = (select auth.uid()))
);
create policy "workspace_asset_insert_member" on storage.objects for insert to authenticated with check (
  bucket_id in ('workspace-assets','exports')
  and exists (select 1 from public.workspace_members wm where wm.workspace_id::text = (storage.foldername(name))[1] and wm.user_id = (select auth.uid()))
);
create policy "workspace_asset_update_member" on storage.objects for update to authenticated using (
  bucket_id in ('workspace-assets','exports')
  and exists (select 1 from public.workspace_members wm where wm.workspace_id::text = (storage.foldername(name))[1] and wm.user_id = (select auth.uid()))
) with check (
  bucket_id in ('workspace-assets','exports')
  and exists (select 1 from public.workspace_members wm where wm.workspace_id::text = (storage.foldername(name))[1] and wm.user_id = (select auth.uid()))
);
create policy "workspace_asset_delete_member" on storage.objects for delete to authenticated using (
  bucket_id in ('workspace-assets','exports')
  and exists (select 1 from public.workspace_members wm where wm.workspace_id::text = (storage.foldername(name))[1] and wm.user_id = (select auth.uid()))
);

create policy "template_preview_public_read" on storage.objects for select to anon, authenticated using (bucket_id = 'template-previews');


-- Explicit Data API grants: grants expose the tables; RLS above controls rows.
grant usage on schema public to anon, authenticated;
grant select, insert, update on public.profiles to authenticated;
grant select, insert, update, delete on public.workspaces to authenticated;
grant select, insert, delete on public.workspace_members to authenticated;
grant select, insert, update, delete on public.projects to authenticated;
grant select, insert, update, delete on public.tasks to authenticated;
grant select, insert, update, delete on public.clients to authenticated;
grant select, insert, update, delete on public.leads to authenticated;
grant select, insert, update, delete on public.content_items to authenticated;
grant select, insert, update, delete on public.assets to authenticated;
grant select on public.invoices to authenticated;
grant select on public.invoice_items to authenticated;
grant select on public.activity_events to authenticated;
grant select, update on public.notifications to authenticated;
grant select, insert, update, delete on public.device_tokens to authenticated;
grant select, insert, update on public.user_preferences to authenticated;
grant select on public.templates to anon, authenticated;
