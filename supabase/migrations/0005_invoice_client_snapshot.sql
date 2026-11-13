-- Keep the bill-to name on the invoice even if the client directory entry changes.
alter table public.invoices
  add column if not exists client_name text not null default '';
