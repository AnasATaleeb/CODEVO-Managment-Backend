package com.codevo.businessos.common;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
@Service
public class ReferenceLookupService {
    private final NamedParameterJdbcTemplate jdbc;
    public ReferenceLookupService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public Map<String,String> names(String type,List<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        if(ids.size()>100) throw DomainException.invalid("Maximum 100 reference IDs");
        String table,field;
        switch(type) {
            case "clients" -> {table="clients";field="display_name";}
            case "projects" -> {table="projects";field="name";}
            case "users" -> {table="app_users";field="name";}
            case "services" -> {table="service_catalog";field="name";}
            case "project-services" -> {table="project_services";field="custom_name";}
            default -> throw DomainException.invalid("Unsupported reference type");
        }
        Map<String,String> result=new LinkedHashMap<>();
        jdbc.query("select id,"+field+" name from "+table+" where id in (:ids)",Map.of("ids",ids.stream().map(UUID::toString).toList()),
            (org.springframework.jdbc.core.RowCallbackHandler)rs->result.put(rs.getString("id"),rs.getString("name")));
        return result;
    }
}
