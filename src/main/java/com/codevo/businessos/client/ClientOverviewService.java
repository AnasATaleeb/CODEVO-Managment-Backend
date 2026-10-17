package com.codevo.businessos.client;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codevo.businessos.analytics.*;
import com.codevo.businessos.common.*;
@Service @Transactional(readOnly=true)
public class ClientOverviewService {
    private final NamedParameterJdbcTemplate jdbc;private final AnalyticsService analytics;private final ReferenceChecks refs;
    private final com.codevo.businessos.billing.FinancialSummaryService financialSummary;
    public ClientOverviewService(NamedParameterJdbcTemplate jdbc,AnalyticsService analytics,ReferenceChecks refs,com.codevo.businessos.billing.FinancialSummaryService financialSummary) { this.jdbc=jdbc;this.analytics=analytics;this.refs=refs;this.financialSummary=financialSummary; }
    public List<Map<String,Object>> metrics(List<UUID> ids,boolean finance) {
        if(ids.isEmpty()) return List.of();
        if(ids.size()>100) throw DomainException.invalid("Maximum 100 clients");
        var p=Map.of("ids",ids.stream().map(UUID::toString).toList());
        var rows=jdbc.queryForList("select c.id,(select count(*) from projects p where p.client_id=c.id and p.status not in ('COMPLETED','CANCELLED','DRAFT')) active_projects,"+
            "(select min(deadline) from projects where client_id=c.id and status not in ('COMPLETED','CANCELLED')) next_deadline,(select max(created_at) from activity_log where client_id=c.id) last_activity,u.name assigned"+
            " from clients c left join app_users u on u.id=c.assigned_user_id where c.id in (:ids)",p);
        if(finance) {
            var outstanding=jdbc.queryForList("select client_id,currency,sum(balance_due) amount,sum(pending_check_amount) pending_check_coverage,sum(greatest(balance_due-pending_check_amount,0)) uncovered_outstanding from invoices where client_id in (:ids) and status not in ('DRAFT','VOID','CANCELLED') group by client_id,currency",p);
            var recurring=jdbc.queryForList("select client_id,currency,sum(amount/case billing_interval when 'MONTHLY' then 1 when 'QUARTERLY' then 3 when 'SEMI_ANNUAL' then 6 when 'YEARLY' then 12 else custom_interval_months end) amount from subscriptions where client_id in (:ids) and status='ACTIVE' group by client_id,currency",p);
            var renewals=jdbc.queryForList("select client_id,min(renewal_date) next_renewal from subscriptions where client_id in (:ids) and status='ACTIVE' group by client_id",p);
            for(var row:rows) {
                row.put("outstanding",outstanding.stream().filter(m->m.get("client_id").equals(row.get("id"))).toList());
                row.put("mrr",recurring.stream().filter(m->m.get("client_id").equals(row.get("id"))).toList());
                row.put("next_renewal",renewals.stream().filter(m->m.get("client_id").equals(row.get("id"))).map(m->m.get("next_renewal")).findFirst().orElse(null));
            }
        }
        return rows;
    }
    public Map<String,Object> summary(UUID clientId,boolean finance) {
        refs.client(clientId);var f=new AnalyticsFilter();f.setClientId(clientId);
        Map<String,Object> result=new LinkedHashMap<>();result.put("metrics",metrics(List.of(clientId),finance).getFirst());
        if(finance) {result.putAll(analytics.financial(f));result.put("contributions",analytics.clientProfit(f));result.put("financialSummary",financialSummary.summary(clientId,null));}
        return result;
    }
}
