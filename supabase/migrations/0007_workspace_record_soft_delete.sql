-- Keep deletion tombstones so removed records stay removed on other devices.
alter table public.workspace_records
  add column if not exists is_deleted boolean not null default false;
