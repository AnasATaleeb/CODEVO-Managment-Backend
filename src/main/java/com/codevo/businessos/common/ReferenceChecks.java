package com.codevo.businessos.common;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReferenceChecks {
    private final JdbcTemplate jdbc;
    public ReferenceChecks(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void client(UUID id) {
        if(id==null)throw DomainException.missing("Client");
        var rows=jdbc.queryForList("select merged_into_id from clients where id=?"+(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() && !org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly()?" for share":""),id);
        if(rows.isEmpty())throw DomainException.missing("Client");
        if(rows.getFirst().get("merged_into_id")!=null)throw DomainException.conflict("Client was merged; use the active client profile");
    }
    public void user(UUID id) { if(id != null) require("app_users",id); }
    public void service(UUID id) {
        require("service_catalog",id);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("select active from service_catalog where id=?",Boolean.class,id))) throw DomainException.invalid("Service is disabled");
    }
    public void project(UUID id, UUID clientId) {
        if(id == null) return;
        UUID owner = jdbc.query("select client_id from projects where id=?",rs -> rs.next() ? com.codevo.businessos.common.SqlIds.uuid(rs,1) : null,id);
        if(owner == null) throw DomainException.missing("Project");
        client(owner);
        if(clientId != null && !owner.equals(clientId)) throw DomainException.invalid("Project does not belong to this client");
    }
    public UUID projectClient(UUID id) {
        UUID result=jdbc.query("select client_id from projects where id=?",rs -> rs.next()?com.codevo.businessos.common.SqlIds.uuid(rs,1):null,id);
        if(result==null) throw DomainException.missing("Project");
        client(result);
        return result;
    }
    public void projectService(UUID id, UUID projectId) {
        if(id==null) return;
        UUID actual=jdbc.query("select project_id from project_services where id=?",rs->rs.next()?com.codevo.businessos.common.SqlIds.uuid(rs,1):null,id);
        if(actual==null || !actual.equals(projectId)) throw DomainException.invalid("Service does not belong to this project");
    }
    private void require(String table, UUID id) {
        if(id == null || !Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from "+table+" where id=?)",Boolean.class,id))) throw DomainException.missing("Related record");
    }
}
