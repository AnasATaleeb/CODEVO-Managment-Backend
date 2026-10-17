package com.codevo.businessos.analytics;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class DashboardService {
    private final JdbcTemplate jdbc;private final AnalyticsService analytics;
    private final com.codevo.businessos.billing.FinancialSummaryService financialSummary;
    public DashboardService(JdbcTemplate jdbc,AnalyticsService analytics,com.codevo.businessos.billing.FinancialSummaryService financialSummary) { this.jdbc=jdbc;this.analytics=analytics;this.financialSummary=financialSummary; }
    public Map<String,Object> summary(AnalyticsFilter f,boolean finance,boolean sales) {
        f.validate();Map<String,Object> out=new LinkedHashMap<>();
        out.put("activeClients",jdbc.queryForObject("select count(*) from clients c where status='ACTIVE' and (? is null or c.id=?) and (? is null or exists(select 1 from projects p where p.client_id=c.id and p.id=?))",Long.class,f.getClientId(),f.getClientId(),f.getProjectId(),f.getProjectId()));
        out.put("activeProjects",jdbc.queryForObject("select count(*) from projects where status not in ('DRAFT','COMPLETED','CANCELLED') and (? is null or client_id=?) and (? is null or id=?)",Long.class,f.getClientId(),f.getClientId(),f.getProjectId(),f.getProjectId()));
        out.put("openTasks",jdbc.queryForObject("select count(*) from tasks where status not in ('DONE','CANCELLED') and (? is null or client_id=?) and (? is null or project_id=?)",Long.class,f.getClientId(),f.getClientId(),f.getProjectId(),f.getProjectId()));
        if(finance) { out.putAll(analytics.financial(f));out.put("clientProfit",analytics.clientProfit(f));out.put("financialSummary",financialSummary.summary(f.getClientId(),f.getProjectId())); }
        if(sales) out.put("sales",analytics.sales(f));
        return out;
    }
}
