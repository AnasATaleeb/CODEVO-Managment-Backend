create table project_billing_milestones (
 id char(36) primary key, project_id char(36) not null, name varchar(255) not null,
 currency varchar(3) not null, due_date date not null, trigger_label varchar(255),
 status varchar(20) not null default 'PLANNED', version bigint not null default 0,
 cancellation_reason varchar(1000), created_at datetime(6) not null default current_timestamp(6),
 foreign key(project_id) references projects(id),
 check(currency in ('USD','ILS','JOD')), check(status in ('PLANNED','CANCELLED'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index milestone_project_idx on project_billing_milestones(project_id,due_date,id);
create table project_billing_milestone_items (
 milestone_id char(36) not null, project_service_id char(36) not null, amount numeric(19,3) not null,
 primary key(milestone_id,project_service_id), check(amount>0),
 foreign key(milestone_id) references project_billing_milestones(id),
 foreign key(project_service_id) references project_services(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create table project_billing_milestone_invoices (
 id char(36) primary key, milestone_id char(36) not null, invoice_id char(36) not null unique,
 retired_at datetime(6), created_at datetime(6) not null default current_timestamp(6),
 active_milestone_id char(36) generated always as (case when retired_at is null then milestone_id else null end) stored,
 unique key milestone_current_invoice_idx(active_milestone_id),
 foreign key(milestone_id) references project_billing_milestones(id), foreign key(invoice_id) references invoices(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create table project_closeouts (
 id char(36) primary key, project_id char(36) not null, reason varchar(1000) not null,
 open_tasks bigint not null, undelivered_services bigint not null, file_count bigint not null,
 actor_user_id char(36), created_at datetime(6) not null default current_timestamp(6),
 foreign key(project_id) references projects(id), foreign key(actor_user_id) references app_users(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index closeout_project_idx on project_closeouts(project_id,created_at);
alter table clients add merged_into_id char(36), add merged_at datetime(6),
 add foreign key(merged_into_id) references clients(id);
create index clients_merge_idx on clients(merged_into_id);
create table client_merges (
 id char(36) primary key, source_client_id char(36) not null unique, target_client_id char(36) not null,
 reason varchar(1000) not null, actor_user_id char(36), created_at datetime(6) not null default current_timestamp(6),
 foreign key(source_client_id) references clients(id), foreign key(target_client_id) references clients(id),
 foreign key(actor_user_id) references app_users(id), check(source_client_id<>target_client_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create table client_merge_records (
 merge_id char(36) not null, entity_type varchar(80) not null, entity_id char(36) not null,
 primary key(merge_id,entity_type,entity_id), foreign key(merge_id) references client_merges(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
alter table invoices add original_client_id char(36), add billing_client_name varchar(255), add billing_client_email varchar(255),
 add foreign key(original_client_id) references clients(id);
update invoices i join clients c on c.id=i.client_id
 set i.original_client_id=i.client_id,i.billing_client_name=c.display_name,i.billing_client_email=c.email;
alter table subscriptions add invoice_automation_enabled boolean not null default false,
 add invoice_lead_days integer not null default 7, add invoice_due_days integer not null default 7, add invoice_automation_error varchar(1000),
 add check(invoice_lead_days between 0 and 90), add check(invoice_due_days between 0 and 90);
create index subscription_automation_idx on subscriptions(invoice_automation_enabled,status,renewal_date);
