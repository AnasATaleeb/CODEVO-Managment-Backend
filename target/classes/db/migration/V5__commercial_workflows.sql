-- Additive commercial fields; legacy price/cost remain the authoritative unit values.
-- V1 used a database-generated CHECK name. Resolve it rather than assuming a name.
set @commercial_billing_check=(select tc.constraint_name from information_schema.table_constraints tc join information_schema.check_constraints cc on cc.constraint_schema=tc.constraint_schema and cc.constraint_name=tc.constraint_name where tc.table_schema=database() and tc.table_name='project_services' and cc.check_clause like '%`billing_type`%' limit 1);
set @commercial_billing_sql=if(@commercial_billing_check is null,'select 1',concat('alter table project_services drop check `',replace(@commercial_billing_check,'`','``'),'`'));
prepare commercial_billing_statement from @commercial_billing_sql;
execute commercial_billing_statement;
deallocate prepare commercial_billing_statement;
alter table project_services add constraint project_services_billing_type_ck check (billing_type in ('ONE_TIME','MONTHLY','QUARTERLY','SEMI_ANNUAL','YEARLY','INSTALLMENT','SETUP_PLUS_RECURRING','CUSTOM'));
alter table project_services add column billing_interval varchar(30), add column custom_interval_months integer;
update project_services set billing_interval=billing_type where billing_type in ('MONTHLY','YEARLY');
alter table project_services add constraint project_services_interval_ck check (billing_interval is null or billing_interval in ('MONTHLY','QUARTERLY','SEMI_ANNUAL','YEARLY','CUSTOM'));
alter table project_services add constraint project_services_custom_interval_ck check (custom_interval_months is null or custom_interval_months between 1 and 120);
-- Renewal invoices are financial snapshots, but do not consume the initial service agreement twice.
alter table invoice_items add column commercial_billing boolean not null default true;
create index invoice_items_service_invoice_idx on invoice_items(project_service_id,invoice_id);

create view project_service_commercial as
select ps.id,ps.project_id,ps.currency,ps.status,
       round(ps.price*ps.quantity,case when ps.currency='JOD' then 3 else 2 end) agreed_amount,
       round(ps.cost*ps.quantity,case when ps.currency='JOD' then 3 else 2 end) estimated_total_cost,
       coalesce(b.invoiced_amount,0) invoiced_amount,
       coalesce(b.draft_reserved_amount,0) draft_reserved_amount,
       greatest(round(ps.price*ps.quantity,case when ps.currency='JOD' then 3 else 2 end)-coalesce(b.invoiced_amount,0),0) uninvoiced_amount,
       greatest(round(ps.price*ps.quantity,case when ps.currency='JOD' then 3 else 2 end)-coalesce(b.invoiced_amount,0)-coalesce(b.draft_reserved_amount,0),0) available_to_invoice
from project_services ps
left join (
    select ii.project_service_id,
           sum(case when i.status not in ('DRAFT','VOID','CANCELLED') then ii.line_total else 0 end) invoiced_amount,
           sum(case when i.status='DRAFT' then ii.line_total else 0 end) draft_reserved_amount
    from invoice_items ii join invoices i on i.id=ii.invoice_id
    where ii.commercial_billing=true and i.status not in ('VOID','CANCELLED')
    group by ii.project_service_id
) b on b.project_service_id=ps.id;
