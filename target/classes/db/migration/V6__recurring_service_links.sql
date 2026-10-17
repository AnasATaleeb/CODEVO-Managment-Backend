-- Existing subscriptions and service links remain intact. Each renewal invoice is an immutable period snapshot.
create table subscription_renewal_invoices (
 id char(36) primary key,
 subscription_id char(36) not null,
 invoice_id char(36) not null unique,
 period_start date not null,
 period_end date not null,
 created_at datetime(6) not null,
 unique key subscription_renewal_period_uk (subscription_id,period_start),
 foreign key (subscription_id) references subscriptions(id),
 foreign key (invoice_id) references invoices(id),
 check (period_end>=period_start)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index subscription_service_status_idx on subscriptions(project_service_id,status);
create index invoices_currency_due_idx on invoices(currency,due_date,status);
