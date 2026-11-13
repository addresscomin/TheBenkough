-- Stable per-record identity for safe multi-device workspace synchronization.
alter table public.workspace_records
  add column if not exists record_key text not null default '';

update public.workspace_records
set record_key = gen_random_uuid()::text
where record_key = '';

alter table public.workspace_records
  drop constraint if exists workspace_records_user_id_module_local_id_key;

create unique index if not exists workspace_records_user_record_key_uidx
  on public.workspace_records(user_id, record_key);
