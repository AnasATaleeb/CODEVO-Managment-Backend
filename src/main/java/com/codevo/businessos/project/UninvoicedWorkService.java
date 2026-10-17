package com.codevo.businessos.project;

import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.ListFilter;
import com.codevo.businessos.common.SqlIds;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class UninvoicedWorkService {
    private final JdbcTemplate jdbc;
    public UninvoicedWorkService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ProjectServiceDtos.UninvoicedPage list(ListFilter f) {
        f.pageable(Set.of("createdAt","customName","status","deadline","projectId"));
        StringBuilder where=new StringBuilder(" where s.status<>'CANCELLED' and c.uninvoiced_amount>0");
        List<Object> params=new ArrayList<>();
        if(f.getClientId()!=null) {where.append(" and p.client_id=?");params.add(f.getClientId());}
        if(f.getProjectId()!=null) {where.append(" and s.project_id=?");params.add(f.getProjectId());}
        if(f.getStatus()!=null && !f.getStatus().isBlank()) {where.append(" and s.status=?");params.add(f.getStatus());}
        if(!f.getQ().isBlank()) {where.append(" and (s.custom_name like ? escape '=' or p.name like ? escape '=' or cl.display_name like ? escape '=')");String q=ListFilter.escape(f.getQ())+"%";params.add(q);params.add(q);params.add(q);}
        if(f.getFrom()!=null) {where.append(" and s.deadline>=?");params.add(f.getFrom());}
        if(f.getTo()!=null) {where.append(" and s.deadline<=?");params.add(f.getTo());}
        String tables=" from project_service_commercial c join project_services s on s.id=c.id join projects p on p.id=s.project_id join clients cl on cl.id=p.client_id";
        long total=Objects.requireNonNull(jdbc.queryForObject("select count(*)"+tables+where,Long.class,params.toArray()));
        var totals=jdbc.query("select c.currency,sum(c.uninvoiced_amount) amount,sum(c.available_to_invoice) available_amount,sum(c.draft_reserved_amount) draft_reserved_amount"+tables+where+" group by c.currency order by c.currency",
            (rs,row)->new ProjectServiceDtos.UninvoicedTotal(Currency.valueOf(rs.getString("currency")),rs.getBigDecimal("amount"),rs.getBigDecimal("available_amount"),rs.getBigDecimal("draft_reserved_amount")),params.toArray());
        String sort=switch(f.getSort()) {case "customName"->"s.custom_name";case "status"->"s.status";case "deadline"->"s.deadline";case "projectId"->"s.project_id";default->"s.created_at";};
        List<Object> pageParams=new ArrayList<>(params);pageParams.add(f.getSize());pageParams.add((long)f.getPage()*f.getSize());
        var rows=jdbc.query("select s.id,s.project_id,p.name project_name,p.client_id,cl.display_name client_name,s.custom_name,c.currency,c.agreed_amount,c.invoiced_amount,c.uninvoiced_amount,c.available_to_invoice,c.draft_reserved_amount,s.status"+tables+where+" order by "+sort+" "+f.getDirection()+",s.id limit ? offset ?",
            (rs,row)->new ProjectServiceDtos.UninvoicedItem(SqlIds.uuid(rs,"id"),SqlIds.uuid(rs,"project_id"),rs.getString("project_name"),SqlIds.uuid(rs,"client_id"),rs.getString("client_name"),rs.getString("custom_name"),Currency.valueOf(rs.getString("currency")),rs.getBigDecimal("agreed_amount"),rs.getBigDecimal("invoiced_amount"),rs.getBigDecimal("uninvoiced_amount"),rs.getBigDecimal("available_to_invoice"),rs.getBigDecimal("draft_reserved_amount"),ProjectCommercialPolicy.billingStatus(rs.getBigDecimal("agreed_amount"),new ProjectCommercialPolicy.BillingAmounts(rs.getBigDecimal("invoiced_amount"),rs.getBigDecimal("draft_reserved_amount"))),rs.getString("status")),pageParams.toArray());
        return new ProjectServiceDtos.UninvoicedPage(rows,f.getPage(),f.getSize(),total,(int)((total+f.getSize()-1)/f.getSize()),totals);
    }
}
