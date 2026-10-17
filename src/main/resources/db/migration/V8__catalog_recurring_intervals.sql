-- Extend catalog terms without rewriting already-applied commercial migrations.
set @catalog_billing_check=(select tc.constraint_name from information_schema.table_constraints tc join information_schema.check_constraints cc on cc.constraint_schema=tc.constraint_schema and cc.constraint_name=tc.constraint_name where tc.table_schema=database() and tc.table_name='service_catalog' and cc.check_clause like '%`default_billing_type`%' limit 1);
set @catalog_billing_sql=if(@catalog_billing_check is null,'select 1',concat('alter table service_catalog drop check `',replace(@catalog_billing_check,'`','``'),'`'));
prepare catalog_billing_statement from @catalog_billing_sql;
execute catalog_billing_statement;
deallocate prepare catalog_billing_statement;
alter table service_catalog add constraint service_catalog_billing_type_ck check (default_billing_type in ('ONE_TIME','MONTHLY','QUARTERLY','SEMI_ANNUAL','YEARLY','INSTALLMENT','SETUP_PLUS_RECURRING','CUSTOM'));
