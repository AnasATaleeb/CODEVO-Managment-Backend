-- Keep voided period snapshots while enforcing one current generation per subscription period.
alter table subscription_renewal_invoices add column retired_at datetime(6);
alter table subscription_renewal_invoices add column active_period_start date
 generated always as (case when retired_at is null then period_start else null end) stored;
-- Keep a standalone FK-supporting index before replacing the original unique index.
create index subscription_renewal_subscription_idx on subscription_renewal_invoices(subscription_id);
alter table subscription_renewal_invoices drop index subscription_renewal_period_uk;
alter table subscription_renewal_invoices add unique key subscription_active_renewal_period_uk (subscription_id,active_period_start);
create index subscription_renewal_history_idx on subscription_renewal_invoices(subscription_id,period_start,created_at);
