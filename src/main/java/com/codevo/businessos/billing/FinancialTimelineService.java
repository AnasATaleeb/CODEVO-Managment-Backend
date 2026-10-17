package com.codevo.businessos.billing;

import com.codevo.businessos.common.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class FinancialTimelineService {
    private final NamedParameterJdbcTemplate jdbc;
    public FinancialTimelineService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PageResponse<Map<String,Object>> list(UUID invoiceId,ListFilter f) {
        var page=f.pageable(Set.of("createdAt"));
        var p=new MapSqlParameterSource().addValue("invoice",SqlIds.text(invoiceId)).addValue("client",SqlIds.text(f.getClientId()))
            .addValue("project",SqlIds.text(f.getProjectId())).addValue("limit",f.getSize()).addValue("offset",page.getOffset());
        String joins=" from activity_log a left join app_users u on u.id=a.actor_user_id"+
            " left join payments p on a.entity_type='payments' and p.id=a.entity_id"+
            " left join check_payments c on a.entity_type='checks' and c.id=a.entity_id"+
            " left join payment_parts pp on pp.id=c.payment_part_id left join payments cp on cp.id=pp.payment_id"+
            " left join invoices i on i.id=case when a.entity_type='invoices' then a.entity_id else coalesce(p.invoice_id,cp.invoice_id) end"+
            " left join subscriptions s on a.entity_type='subscriptions' and s.id=a.entity_id"+
            " left join expenses e on a.entity_type='expenses' and e.id=a.entity_id";
        String where=" where a.entity_type in ('invoices','payments','checks','subscriptions','expenses')"+
            " and (:invoice is null or i.id=:invoice) and (:client is null or a.client_id=:client)"+
            " and (:project is null or coalesce(i.project_id,s.project_id,e.project_id)=:project)";
        long count=jdbc.queryForObject("select count(*)"+joins+where,p,Long.class);
        var rows=jdbc.queryForList("select a.id,a.entity_type,a.entity_id,a.client_id,a.actor_user_id,u.name actor_name,a.action,a.summary,a.created_at"+
            joins+where+" order by a.created_at "+f.getDirection()+",a.id limit :limit offset :offset",p);
        return new PageResponse<>(rows,f.getPage(),f.getSize(),count,(int)((count+f.getSize()-1)/f.getSize()));
    }
}
