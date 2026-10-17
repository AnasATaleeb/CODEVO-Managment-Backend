package com.codevo.businessos.project;

import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.SqlIds;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Batched reads of the same commercial view used by financial summaries. */
@Service
public class ProjectCommercialReadService {
    private final JdbcTemplate jdbc;
    public ProjectCommercialReadService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public Map<UUID,ProjectCommercialPolicy.BillingAmounts> billingAmounts(Collection<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        Map<UUID,ProjectCommercialPolicy.BillingAmounts> values=new HashMap<>();
        jdbc.query("select id,invoiced_amount,draft_reserved_amount from project_service_commercial where id in ("+placeholders(ids.size())+")",
            rs->{ values.put(SqlIds.uuid(rs,"id"),new ProjectCommercialPolicy.BillingAmounts(rs.getBigDecimal("invoiced_amount"),rs.getBigDecimal("draft_reserved_amount"))); },ids.toArray());
        return values;
    }

    /** Locking/current read: unlike a snapshot query, observes invoice writes that committed while waiting for the service lock. */
    public Map<UUID,ProjectCommercialPolicy.BillingAmounts> currentBillingAmounts(Collection<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        Map<UUID,ProjectCommercialPolicy.BillingAmounts> values=new HashMap<>();
        jdbc.query("select ii.project_service_id,ii.line_total,i.status from invoice_items ii join invoices i on i.id=ii.invoice_id where ii.commercial_billing=true and ii.project_service_id in ("+placeholders(ids.size())+") order by i.id,ii.id for update",
            rs->{
                String status=rs.getString("status");if(Set.of("VOID","CANCELLED").contains(status))return;
                UUID id=SqlIds.uuid(rs,"project_service_id");var previous=values.getOrDefault(id,ProjectCommercialPolicy.BillingAmounts.none());
                BigDecimal amount=rs.getBigDecimal("line_total");
                values.put(id,"DRAFT".equals(status)?new ProjectCommercialPolicy.BillingAmounts(previous.invoicedAmount(),previous.draftReservedAmount().add(amount)):new ProjectCommercialPolicy.BillingAmounts(previous.invoicedAmount().add(amount),previous.draftReservedAmount()));
            },ids.toArray());
        return values;
    }

    public Map<UUID,List<ProjectDtos.CommercialTotal>> projectTotals(Collection<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        Map<UUID,List<ProjectDtos.CommercialTotal>> values=new HashMap<>();
        jdbc.query("select project_id,currency,sum(case when status<>'CANCELLED' then agreed_amount else 0 end) contract_value,sum(case when status<>'CANCELLED' then estimated_total_cost else 0 end) estimated_cost,sum(case when status<>'CANCELLED' then 1 else 0 end) service_count from project_service_commercial where project_id in ("+placeholders(ids.size())+") group by project_id,currency order by currency",
            rs->{ values.computeIfAbsent(SqlIds.uuid(rs,"project_id"),key->new ArrayList<>()).add(new ProjectDtos.CommercialTotal(Currency.valueOf(rs.getString("currency")),rs.getBigDecimal("contract_value"),rs.getBigDecimal("estimated_cost"),rs.getLong("service_count"))); },ids.toArray());
        return values;
    }

    private String placeholders(int count) { return String.join(",",Collections.nCopies(count,"?")); }
}
