create table organization_settings (
 id integer primary key check(id=1),company_name varchar(255) not null,logo_url varchar(2048),
 default_currency varchar(3) not null check(default_currency in ('USD','ILS','JOD')),reporting_currency varchar(3) not null check(reporting_currency in ('USD','ILS','JOD')),
 timezone varchar(80) not null,invoice_prefix varchar(20) not null,payment_warning_days integer not null check(payment_warning_days between 0 and 90),
 subscription_warning_days integer not null check(subscription_warning_days between 0 and 90),deadline_warning_days integer not null check(deadline_warning_days between 0 and 90),
 version bigint not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
insert into organization_settings values(1,'CODEVO',null,'USD','USD','Asia/Hebron','INV',3,30,3,0);
create table notifications (
 id char(36) primary key,user_id char(36) not null,type varchar(80) not null,title varchar(255) not null,message varchar(1000) not null,
 entity_type varchar(80) not null,entity_id char(36) not null,route varchar(255) not null,priority integer not null,
 dedup_key varchar(255) not null,read_at datetime(6),resolved_at datetime(6),created_at datetime(6) not null default current_timestamp(6),unique(user_id,dedup_key)
, foreign key (user_id) references app_users(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index notifications_user_unread_idx on notifications(user_id,resolved_at,read_at,created_at desc);
create table file_metadata (
 id char(36) primary key,client_id char(36),project_id char(36),invoice_id char(36),
 expense_id char(36),task_id char(36),payment_id char(36),
 category varchar(80) not null,original_name varchar(255) not null,storage_key varchar(255) not null unique,content_type varchar(100) not null,size bigint not null check(size>0 and size<=20971520),
 created_at datetime(6) not null default current_timestamp(6),created_by char(36),
 check(client_id is not null or project_id is not null or invoice_id is not null or expense_id is not null or task_id is not null or payment_id is not null)
, foreign key (client_id) references clients(id), foreign key (project_id) references projects(id), foreign key (invoice_id) references invoices(id), foreign key (expense_id) references expenses(id), foreign key (task_id) references tasks(id), foreign key (payment_id) references payments(id), foreign key (created_by) references app_users(id)) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
create index files_client_idx on file_metadata(client_id);
create index files_project_idx on file_metadata(project_id);
alter table payment_parts add foreign key(attachment_file_id) references file_metadata(id);
alter table check_payments add foreign key(attachment_file_id) references file_metadata(id);
create index leads_followup_idx on leads(stage,next_follow_up_at);
create index subscriptions_period_idx on subscriptions(status,current_period_end);
