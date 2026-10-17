-- Optional issuer names must not bypass duplicate physical-check protection.
alter table check_payments add column issuer_identity varchar(255)
    generated always as (coalesce(trim(issuer_name),'')) stored;
create unique index check_physical_identity_idx on check_payments(bank_name,check_number,issuer_identity);
create index client_list_idx on clients(status,created_at,id);
create index project_list_idx on projects(client_id,status,created_at,id);
create index task_list_idx on tasks(project_id,status,created_at,id);
create index subscription_list_idx on subscriptions(client_id,status,renewal_date,id);
create index expense_list_idx on expenses(client_id,expense_date,id);
