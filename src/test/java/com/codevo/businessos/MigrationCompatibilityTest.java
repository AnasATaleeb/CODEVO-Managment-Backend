package com.codevo.businessos;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Upgrade real V4 records in a separate, empty native MySQL test schema. Never cleans a schema. */
class MigrationCompatibilityTest {
    @Test void additiveCommercialMigrationsPreserveLegacySnapshotsAndNullableLinks() throws Exception {
        String url=System.getenv("TEST_MIGRATION_DATABASE_URL");
        assumeTrue(url!=null,"Supply a separate empty TEST_MIGRATION_DATABASE_URL");
        if(!url.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost)(:[0-9]+)?/[a-zA-Z0-9_]+_test(\\?.*)?") || url.equals(System.getenv("TEST_DATABASE_URL")))
            throw new IllegalArgumentException("Migration verification requires a separate loopback test schema");
        String user=System.getenv("TEST_DATABASE_USERNAME"),password=System.getenv("TEST_DATABASE_PASSWORD");
        try(var connection=DriverManager.getConnection(url,user,password)) {
            try(var rs=connection.getMetaData().getTables(connection.getCatalog(),null,"%",new String[]{"TABLE"})) { assertThat(rs.next()).as("Use a fresh empty schema for upgrade verification").isFalse(); }
        }
        Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration").target("4").load().migrate();
        String client=id(),project=id(),catalog=id(),service=id(),invoice=id(),item=id(),subscription=id();
        try(var c=DriverManager.getConnection(url,user,password)) {
            insert(c,"clients",true,"id",client,"display_name","Legacy client","status","ACTIVE","preferred_currency","ILS");
            insert(c,"projects",true,"id",project,"client_id",client,"name","Legacy project","project_code","LEGACY-UPGRADE","status","IN_PROGRESS","priority","NORMAL","estimated_price",9999,"estimated_cost",999,"currency","ILS");
            insert(c,"service_catalog",true,"id",catalog,"name","Legacy hosting","code","LEGACY-HOSTING","category","Hosting","default_billing_type","YEARLY","default_price",300,"default_cost",100,"default_currency","ILS","active",true);
            insert(c,"project_services",true,"id",service,"project_id",project,"service_catalog_item_id",catalog,"custom_name","Legacy hosting","billing_type","YEARLY","price",300,"cost",100,"quantity",1,"currency","ILS","status","DELIVERED","recurring",true);
            insert(c,"invoices",true,"id",invoice,"client_id",client,"project_id",project,"invoice_number","LEGACY-001","status","ISSUED","issue_date","2026-09-01","due_date","2026-10-01","currency","ILS","subtotal",100,"discount",0,"tax",0,"total",100,"balance_due",100);
            insert(c,"invoice_items",false,"id",item,"invoice_id",invoice,"project_service_id",null,"description","Legacy manual snapshot","quantity",1,"unit_price",100,"line_total",100);
            insert(c,"subscriptions",true,"id",subscription,"client_id",client,"project_id",project,"project_service_id",null,"name","Legacy unlinked subscription","status","ACTIVE","billing_interval","YEARLY","amount",300,"currency","ILS","start_date","2026-01-01","current_period_start","2026-01-01","current_period_end","2026-12-31","renewal_date","2027-01-01","auto_renew",false,"grace_period_days",0);
        }
        Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration").load().migrate();
        try(var c=DriverManager.getConnection(url,user,password);var stmt=c.createStatement()) {
            try(var rs=stmt.executeQuery("select estimated_price,estimated_cost from projects")) { assertThat(rs.next()).isTrue();assertThat(rs.getBigDecimal(1)).isEqualByComparingTo("9999");assertThat(rs.getBigDecimal(2)).isEqualByComparingTo("999"); }
            try(var rs=stmt.executeQuery("select price,billing_interval from project_services")) { rs.next();assertThat(rs.getBigDecimal(1)).isEqualByComparingTo("300");assertThat(rs.getString(2)).isEqualTo("YEARLY"); }
            try(var rs=stmt.executeQuery("select project_service_id,description,line_total,commercial_billing from invoice_items")) { rs.next();assertThat(rs.getString(1)).isNull();assertThat(rs.getString(2)).isEqualTo("Legacy manual snapshot");assertThat(rs.getBigDecimal(3)).isEqualByComparingTo("100");assertThat(rs.getBoolean(4)).isTrue(); }
            try(var rs=stmt.executeQuery("select project_service_id,amount,renewal_date from subscriptions")) { rs.next();assertThat(rs.getString(1)).isNull();assertThat(rs.getBigDecimal(2)).isEqualByComparingTo("300");assertThat(rs.getDate(3).toString()).isEqualTo("2027-01-01"); }
            try(var rs=stmt.executeQuery("select count(*) from flyway_schema_history where success=1")) { rs.next();assertThat(rs.getInt(1)).isEqualTo(9); }
        }
    }
    private String id() { return UUID.randomUUID().toString(); }
    private void insert(Connection c,String table,boolean audit,Object... fields) throws SQLException {
        Map<String,Object> values=new LinkedHashMap<>();for(int i=0;i<fields.length;i+=2)values.put((String)fields[i],fields[i+1]);
        if(audit) { values.put("created_at",Timestamp.valueOf("2026-09-01 00:00:00"));values.put("updated_at",Timestamp.valueOf("2026-09-01 00:00:00")); }
        String sql="insert into "+table+" ("+String.join(",",values.keySet())+") values ("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
        try(var stmt=c.prepareStatement(sql)) { int index=1;for(Object value:values.values())stmt.setObject(index++,value);stmt.executeUpdate(); }
    }
}
