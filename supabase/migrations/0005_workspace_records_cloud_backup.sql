-- Private per-user cloud backup for the app's local workspace records.
create table if not exists public.workspace_records (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  local_id bigint not null,
  module text not null check (char_length(module) between 1 and 64),
  title text not null check (char_length(title) between 1 and 240),
  details text not null default '',
  local_updated_at bigint not null,
  synced_at timestamptz not null default now(),
  unique (user_id, module, local_id)
);

alter table public.workspace_records enable row level security;
revoke all on public.workspace_records from anon, public;
grant select, insert, update, delete on public.workspace_records to authenticated;
grant usage on schema public to authenticated;

drop policy if exists workspace_records_select_own on public.workspace_records;
create policy workspace_records_select_own on public.workspace_records
for select to authenticated using (user_id = (select auth.uid()));
drop policy if exists workspace_records_insert_own on public.workspace_records;
create policy workspace_records_insert_own on public.workspace_records
for insert to authenticated with check (user_id = (select auth.uid()));
drop policy if exists workspace_records_update_own on public.workspace_records;
create policy workspace_records_update_own on public.workspace_records
for update to authenticated using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));
drop policy if exists workspace_records_delete_own on public.workspace_records;
create policy workspace_records_delete_own on public.workspace_records
for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists benkough_block_anonymous_sessions on public.workspace_records;
create policy benkough_block_anonymous_sessions on public.workspace_records
as restrictive for all to authenticated
using ((select auth.jwt() ->> 'is_anonymous') is distinct from 'true')
with check ((select auth.jwt() ->> 'is_anonymous') is distinct from 'true');
