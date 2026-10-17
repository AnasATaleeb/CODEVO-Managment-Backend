package com.codevo.businessos.analytics;

import java.util.*;
import java.math.BigDecimal;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.common.*;

@Service @Transactional(readOnly=true)
public class AnalyticsService {
    private final NamedParameterJdbcTemplate jdbc;
    private final BusinessDates dates;
    public AnalyticsService(NamedParameterJdbcTemplate jdbc,BusinessDates dates) { this.jdbc=jdbc;this.dates=dates; }
    private MapSqlParameterSource params(AnalyticsFilter f) {
        f.validate();return new MapSqlParameterSource().addValue("from",f.getFrom()).addValue("to",f.getTo()).addValue("client",SqlIds.text(f.getClientId())).addValue("project",SqlIds.text(f.getProjectId())).addValue("today",dates.today());
    }
    private String scope(String alias,String date) {
        return " (:client is null or "+alias+".client_id=:client) and (:project is null or "+alias+(alias.equals("p")?".id":".project_id")+"=:project)"+
            (date==null?"":" and (:from is null or "+date+">=:from) and (:to is null or "+date+"<=:to)");
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public Map<String,Object> financial(AnalyticsFilter f) {
        var p=params(f);String ledgerScope=scope("l","l.collected_date");
        Map<String,Object> result=new LinkedHashMap<>();
        for(String metric:List.of("collected","pendingChecks","clearedChecks","bouncedChecks")) {
            String condition=switch(metric){case "collected"->"collectible";case "pendingChecks"->"pending";case "clearedChecks"->"collectible and method='CHECK'";default->"bounced";};
            String date=metric.equals("pendingChecks")?null:metric.equals("bouncedChecks")?"l.transaction_date":"l.collected_date";
            result.put(metric,jdbc.queryForList("select currency,sum(amount) amount from ledger_parts l where "+condition+" and "+scope("l",date)+" group by currency order by currency",p));
        }
        result.put("expenses",jdbc.queryForList("select currency,sum(amount) amount from expenses e where "+scope("e","e.expense_date")+" group by currency order by currency",p));
        result.put("invoiced",invoiceMetric("total","status not in ('DRAFT','VOID','CANCELLED')",f));
        result.put("outstanding",invoiceStock("status not in ('DRAFT','VOID','CANCELLED')",f));
        result.put("overdue",invoiceStock("status not in ('DRAFT','VOID','CANCELLED') and due_date<:today and balance_due>0",f));
        result.put("uncoveredOutstanding",jdbc.queryForList("select currency,sum(greatest(balance_due-pending_check_amount,0)) amount from invoices i where status not in ('DRAFT','VOID','CANCELLED') and "+scope("i",null)+" group by currency order by currency",p));
        result.put("pendingCheckCoverage",jdbc.queryForList("select currency,sum(pending_check_amount) amount from invoices i where status not in ('DRAFT','VOID','CANCELLED') and "+scope("i",null)+" group by currency order by currency",p));
        result.put("uninvoiced",jdbc.queryForList("select ps.currency,sum(ps.uninvoiced_amount) amount from project_service_commercial ps join projects p on p.id=ps.project_id where ps.status<>'CANCELLED' and p.status<>'CANCELLED' and "+scope("p",null)+" group by ps.currency order by ps.currency",p));
        result.put("contracted",jdbc.queryForList("select currency,sum(amount) amount from (select ps.currency,ps.agreed_amount amount from project_service_commercial ps join projects p on p.id=ps.project_id where ps.status<>'CANCELLED' and p.status<>'CANCELLED' and "+scope("p","p.start_date")+
            " union all select p.currency,p.estimated_price from projects p where p.status<>'CANCELLED' and "+scope("p","p.start_date")+" and not exists(select 1 from project_services ps where ps.project_id=p.id)) contracted group by currency order by currency",p));
        var recurring=jdbc.queryForList("select currency,sum(amount / case billing_interval when 'MONTHLY' then 1 when 'QUARTERLY' then 3 when 'SEMI_ANNUAL' then 6 when 'YEARLY' then 12 else custom_interval_months end) amount from subscriptions s where status='ACTIVE' and "+scope("s",null)+" group by currency order by currency",p);
        result.put("mrr",recurring);result.put("arr",recurring.stream().map(m->Map.of("currency",m.get("currency"),"amount",((BigDecimal)m.get("amount")).multiply(BigDecimal.valueOf(12)))).toList());
        result.put("profit",jdbc.queryForList("select currency,sum(amount) amount from ("+
            "select currency,amount from ledger_parts l where collectible and "+ledgerScope+" union all select currency,-amount from expenses e where "+scope("e","e.expense_date")+
            ") cash group by currency order by currency",p));
        result.put("trend",jdbc.queryForList("select date_format(collected_date,'%Y-%m-01') month,currency,sum(amount) collected from ledger_parts l where collectible and "+ledgerScope+" group by 1,2 order by 1,2",p));
        result.put("byMethod",groupCollection(f,"method"));result.put("byBank",groupCollection(f,"coalesce(bank_name,'Unspecified')"));
        result.put("byService",jdbc.queryForList("with collected as (select invoice_id,currency,sum(amount) amount from ledger_parts l where collectible and "+ledgerScope+" group by invoice_id,currency),weights as ("+
            "select ii.invoice_id,coalesce(sc.name,'Other services') name,sum(ii.line_total) weight from invoice_items ii left join project_services ps on ps.id=ii.project_service_id left join service_catalog sc on sc.id=ps.service_catalog_item_id group by ii.invoice_id,sc.name) "+
            "select w.name,c.currency,sum(c.amount*w.weight/nullif(i.subtotal,0)) amount from collected c join invoices i on i.id=c.invoice_id join weights w on w.invoice_id=c.invoice_id group by w.name,c.currency order by amount desc limit 30",p));
        result.put("byCategory",jdbc.queryForList("select category name,currency,sum(amount) amount from expenses e where "+scope("e","e.expense_date")+" group by category,currency order by amount desc limit 30",p));
        result.put("expenseTrend",jdbc.queryForList("select date_format(expense_date,'%Y-%m-01') month,currency,sum(amount) amount from expenses e where "+scope("e","e.expense_date")+" group by 1,2 order by 1,2",p));
        return result;
    }
    private List<Map<String,Object>> invoiceStock(String condition,AnalyticsFilter f) {
        return jdbc.queryForList("select currency,sum(balance_due) amount from invoices i where "+condition+" and "+scope("i",null)+" group by currency order by currency",params(f));
    }
    private List<Map<String,Object>> invoiceMetric(String column,String condition,AnalyticsFilter f) {
        return jdbc.queryForList("select currency,sum("+column+") amount from invoices i where "+condition+" and "+scope("i","i.issue_date")+" group by currency order by currency",params(f));
    }
    private List<Map<String,Object>> groupCollection(AnalyticsFilter f,String column) {
        return jdbc.queryForList("select "+column+" name,currency,sum(amount) amount from ledger_parts l where collectible and "+scope("l","l.collected_date")+" group by 1,2 order by amount desc limit 30",params(f));
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public List<Map<String,Object>> clientProfit(AnalyticsFilter f) {
        return profitReport(f,true);
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public List<Map<String,Object>> projectProfit(AnalyticsFilter f) {
        return profitReport(f,false);
    }
    private List<Map<String,Object>> profitReport(AnalyticsFilter f,boolean client) {
        String dimension=client?"client_id":"project_id";
        String projectDimension=client?"p.client_id":"p.id";
        String entity=client?"clients":"projects";
        String name=client?"display_name":"name";
        return jdbc.queryForList("with cash as (select "+dimension+" entity_id,currency,amount collected,cast(0 as decimal(19,3)) expense from ledger_parts l where collectible and "+dimension+" is not null and "+scope("l","l.collected_date")+
            " union all select "+dimension+",currency,cast(0 as decimal(19,3)),amount from expenses e where "+dimension+" is not null and "+scope("e","e.expense_date")+
            "), grouped as(select entity_id,currency,sum(collected) collected,sum(expense) expenses from cash group by entity_id,currency),"+
            "commercial_rows as (select "+projectDimension+" entity_id,ps.currency,ps.agreed_amount contracted,ps.estimated_total_cost estimated_cost,ps.uninvoiced_amount remaining_to_invoice"+
            " from project_service_commercial ps join projects p on p.id=ps.project_id where ps.status<>'CANCELLED' and p.status<>'CANCELLED' and "+scope("p","p.start_date")+
            " union all select "+projectDimension+",p.currency,p.estimated_price,p.estimated_cost,0 from projects p where p.status<>'CANCELLED' and "+scope("p","p.start_date")+" and not exists(select 1 from project_services ps where ps.project_id=p.id)),"+
            "commercial as(select entity_id,currency,sum(contracted) contracted,sum(estimated_cost) estimated_cost,sum(remaining_to_invoice) remaining_to_invoice from commercial_rows group by entity_id,currency),"+
            "billed as(select "+dimension+" entity_id,currency,sum(total) invoiced,sum(paid_amount) allocated_collected,sum(pending_check_amount) pending_check_coverage,"+
            "sum(balance_due) outstanding_amount,sum(greatest(balance_due-pending_check_amount,0)) uncovered_outstanding from invoices i where status not in ('DRAFT','VOID','CANCELLED') and "+dimension+" is not null and "+scope("i",null)+" group by "+dimension+",currency),"+
            "currency_keys as(select entity_id,currency from grouped union select entity_id,currency from commercial union select entity_id,currency from billed)"+
            " select t.id,t."+name+" name,k.currency,coalesce(c.contracted,0) contracted,coalesce(c.estimated_cost,0) estimated_cost,coalesce(c.remaining_to_invoice,0) remaining_to_invoice,"+
            "coalesce(g.collected,0) collected,coalesce(g.expenses,0) expenses,coalesce(g.expenses,0) actual_cost,coalesce(g.collected,0)-coalesce(g.expenses,0) profit,"+
            "coalesce(c.contracted,0)-coalesce(g.expenses,0) expected_profit,coalesce(b.invoiced,0) invoiced,coalesce(b.allocated_collected,0) allocated_collected,"+
            "coalesce(b.pending_check_coverage,0) pending_check_coverage,coalesce(b.outstanding_amount,0) outstanding_amount,coalesce(b.uncovered_outstanding,0) uncovered_outstanding,"+
            "case when g.collected>0 then (g.collected-g.expenses)/g.collected*100 else null end margin from currency_keys k join "+entity+" t on t.id=k.entity_id"+
            " left join grouped g on g.entity_id=k.entity_id and g.currency=k.currency left join commercial c on c.entity_id=k.entity_id and c.currency=k.currency"+
            " left join billed b on b.entity_id=k.entity_id and b.currency=k.currency order by profit desc,t.id,k.currency limit 100",params(f));
    }
    @PreAuthorize("hasAuthority('CRM_WRITE')")
    public Map<String,Object> sales(AnalyticsFilter f) {
        var p=params(f);String range="(:from is null or created_at>=:from) and (:to is null or created_at<date_add(:to,interval 1 day))";
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("counts",jdbc.queryForMap("select count(*) created,sum(case when stage='WON' then 1 else 0 end) won,sum(case when stage='LOST' then 1 else 0 end) lost,coalesce(100.0*sum(case when stage='WON' then 1 else 0 end)/nullif(count(*),0),0) conversion_rate from leads where "+range,p));
        out.put("pipeline",jdbc.queryForList("select stage,currency,count(*) count,sum(estimated_value) amount from leads where "+range+" group by stage,currency order by stage,currency",p));
        out.put("sources",jdbc.queryForList("select coalesce(source,'Unspecified') name,currency,count(*) count,sum(case when stage='WON' then 1 else 0 end) won,sum(estimated_value) amount from leads where "+range+" group by source,currency order by count desc limit 30",p));
        out.put("lostReasons",jdbc.queryForList("select lost_reason name,count(*) count from leads where stage='LOST' and "+range+" group by lost_reason order by count desc limit 30",p));
        return out;
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public Map<String,Object> clientCounts(AnalyticsFilter f) {
        return jdbc.queryForMap("select count(*) total,sum(case when status='ACTIVE' then 1 else 0 end) active,sum(case when status in ('INACTIVE','LOST') then 1 else 0 end) inactive,sum(case when (:from is null or created_at>=:from) and (:to is null or created_at<date_add(:to,interval 1 day)) then 1 else 0 end) new_clients from clients",params(f));
    }
}
