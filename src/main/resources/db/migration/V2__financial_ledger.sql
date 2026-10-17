create table invoices (
 id char(36) primary key, client_id char(36) not null, project_id char(36),
 invoice_number varchar(255) not null unique, status varchar(30) not null check(status in ('DRAFT','ISSUED','PARTIALLY_PAID','PAID','OVERDUE','VOID','CANCELLED')),
 issue_date date not null, due_date date not null, currency varchar(3) not null check(currency in ('USD','ILS','JOD')),
 subtotal numeric(19,3) not null check(subtotal>=0), discount numeric(19,3) not null check(discount>=0),
 tax numeric(19,3) not null check(tax>=0), total numeric(19,3) not null check(total>0),
 paid_amount numeric(19,3) not null default 0 check(paid_amount>=0),
 pending_check_amount numeric(19,3) not null default 0 check(pending_check_amount>=0),
 balance_due numeric(19,3) not null check(balance_due>=0), notes longtext, void_reason varchar(1000),
 created_at datetime(6) not null,updated_at datetime(6) not null,created_by char(36),updated_by char(36),version bigint not null default 0,
 check(due_date>=issue_date), check(total=subtotal-discount+tax),check(balance_due=total-paid_amount),check(paid_amount<=total),check(paid_amount+pending_check_amount<=total)
, foreign key (client_id) references clients(id), foreign key (project_id) references projects(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create table invoice_items (
 id char(36) primary key,invoice_id char(36) not null,project_service_id char(36),
 description varchar(1000) not null,quantity numeric(12,3) not null check(quantity>0),unit_price numeric(19,3) not null check(unit_price>=0),
 line_total numeric(19,3) not null check(line_total>=0)
, foreign key (invoice_id) references invoices(id), foreign key (project_service_id) references project_services(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index invoice_items_invoice_idx on invoice_items(invoice_id);
create index invoices_client_idx on invoices(client_id);
create index invoices_project_idx on invoices(project_id);
create index invoices_status_due_idx on invoices(status,due_date);
create index invoices_issue_date_idx on invoices(issue_date);
create index invoices_search_idx on invoices(invoice_number(120));
create table payments (
 id char(36) primary key,client_id char(36) not null,invoice_id char(36),payment_date date not null,
 status varchar(30) not null check(status in ('RECORDED','PARTIALLY_SETTLED','SETTLED','REVERSED')),
 notes longtext,reversed_at datetime(6),reversal_reason varchar(1000),
 created_at datetime(6) not null,updated_at datetime(6) not null,created_by char(36),updated_by char(36),version bigint not null default 0
, foreign key (client_id) references clients(id), foreign key (invoice_id) references invoices(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create table payment_parts (
 id char(36) primary key,payment_id char(36) not null,
 method varchar(30) not null check(method in ('CASH','BANK_TRANSFER','CHECK','CARD','PAYPAL','STRIPE','OTHER')),
 amount numeric(19,3) not null check(amount>0),currency varchar(3) not null check(currency in ('USD','ILS','JOD')),
 bank_name varchar(255),reference_number varchar(255),transaction_date date,
 allocation_exchange_rate numeric(19,8) check(allocation_exchange_rate>0),allocation_amount numeric(19,3) check(allocation_amount>0),
 exchange_rate_to_reporting_currency numeric(19,8) check(exchange_rate_to_reporting_currency>0),
 reporting_currency varchar(3) check(reporting_currency in ('USD','ILS','JOD')),reporting_amount numeric(19,3) check(reporting_amount>0),
 notes longtext,attachment_file_id char(36),
 created_at datetime(6) not null,updated_at datetime(6) not null,created_by char(36),updated_by char(36),version bigint not null default 0
, foreign key (payment_id) references payments(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create table check_payments (
 id char(36) primary key,payment_part_id char(36) not null unique,
 check_number varchar(255) not null,bank_name varchar(255) not null,issuer_name varchar(255),
 amount numeric(19,3) not null check(amount>0),currency varchar(3) not null check(currency in ('USD','ILS','JOD')),
 received_date date not null,due_date date not null,deposited_date date,cleared_date date,bounced_date date,
 status varchar(30) not null check(status in ('PENDING','DEPOSITED','CLEARED','BOUNCED','CANCELLED')),
 notes longtext,attachment_file_id char(36),reversed_at datetime(6),reversal_reason varchar(1000),
 created_at datetime(6) not null,updated_at datetime(6) not null,created_by char(36),updated_by char(36),version bigint not null default 0,
 unique(bank_name,check_number,issuer_name),
 check(status<>'CLEARED' or cleared_date is not null),check(status<>'BOUNCED' or bounced_date is not null),
 check(status<>'DEPOSITED' or deposited_date is not null)
, foreign key (payment_part_id) references payment_parts(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index payments_invoice_idx on payments(invoice_id);
create index payments_client_date_idx on payments(client_id,payment_date);
create index payments_date_idx on payments(payment_date);
create index payment_parts_payment_idx on payment_parts(payment_id);
create index check_status_due_idx on check_payments(status,due_date);
create index checks_search_idx on check_payments(check_number(120));
create view ledger_parts as
 select pp.*,p.client_id,p.invoice_id,i.project_id,p.status payment_status,c.id check_id,c.check_number,c.status check_status,c.due_date,c.reversed_at check_reversed_at,
 case when pp.method='CHECK' then c.cleared_date else coalesce(pp.transaction_date,p.payment_date) end collected_date,
 (p.status<>'REVERSED' and (pp.method<>'CHECK' or (c.status='CLEARED' and c.reversed_at is null))) collectible,
 (p.status<>'REVERSED' and pp.method='CHECK' and c.status in ('PENDING','DEPOSITED')) pending,
 (p.status<>'REVERSED' and pp.method='CHECK' and c.status='BOUNCED') bounced
 from payment_parts pp join payments p on p.id=pp.payment_id left join check_payments c on c.payment_part_id=pp.id left join invoices i on i.id=p.invoice_id;
