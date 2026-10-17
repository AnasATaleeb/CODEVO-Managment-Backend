package com.codevo.businessos.billing;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared lifetime read model. Every amount stays in its recorded currency. */
@Service @Transactional(readOnly=true)
public class FinancialSummaryService {
    public record CurrencySummary(Currency currency, BigDecimal contracted, BigDecimal invoiced,
        BigDecimal remainingToInvoice, BigDecimal collected, BigDecimal cashCollected,
        BigDecimal pendingCheckCoverage, BigDecimal outstandingAmount, BigDecimal uncoveredOutstanding,
        BigDecimal estimatedCost, BigDecimal expenses, BigDecimal cashProfit, BigDecimal expectedProfit,
        BigDecimal mrr, BigDecimal arr) {}
    public record Summary(List<CurrencySummary> currencies, LocalDate nextRenewal) {}
    private final NamedParameterJdbcTemplate jdbc;
    public FinancialSummaryService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public Summary summary(UUID clientId, UUID projectId) {
        var p=new MapSqlParameterSource().addValue("client",SqlIds.text(clientId)).addValue("project",SqlIds.text(projectId));
        Map<Currency,Map<String,BigDecimal>> amounts=new TreeMap<>();
        String projectScope="(:client is null or p.client_id=:client) and (:project is null or p.id=:project)";
        add(amounts,jdbc.queryForList("select ps.currency,sum(ps.agreed_amount) contracted,sum(ps.estimated_total_cost) estimated_cost,"+
            "sum(ps.uninvoiced_amount) remaining_to_invoice from project_service_commercial ps join projects p on p.id=ps.project_id"+
            " where ps.status<>'CANCELLED' and p.status<>'CANCELLED' and "+projectScope+" group by ps.currency",p));
        // Preserve estimates on legacy projects that have no commercial services. They are not invoiceable service balances.
        add(amounts,jdbc.queryForList("select p.currency,sum(p.estimated_price) contracted,sum(p.estimated_cost) estimated_cost from projects p"+
            " where p.status<>'CANCELLED' and "+projectScope+" and not exists(select 1 from project_services ps where ps.project_id=p.id) group by p.currency",p));
        add(amounts,jdbc.queryForList("select i.currency,sum(i.total) invoiced,sum(i.paid_amount) collected,sum(i.pending_check_amount) pending_check_coverage,"+
            "sum(i.balance_due) outstanding_amount,sum(greatest(i.balance_due-i.pending_check_amount,0)) uncovered_outstanding from invoices i"+
            " where i.status not in ('DRAFT','VOID','CANCELLED') and "+scope("i")+" group by i.currency",p));
        // Cash uses the original part currency; allocation snapshots belong to the invoice collection metric above.
        add(amounts,jdbc.queryForList("select l.currency,sum(l.amount) cash_collected from ledger_parts l where l.collectible and "+scope("l")+" group by l.currency",p));
        add(amounts,jdbc.queryForList("select e.currency,sum(e.amount) expenses from expenses e where "+scope("e")+" group by e.currency",p));
        add(amounts,jdbc.queryForList("select s.currency,sum(s.amount/nullif(case s.billing_interval when 'MONTHLY' then 1 when 'QUARTERLY' then 3"+
            " when 'SEMI_ANNUAL' then 6 when 'YEARLY' then 12 else s.custom_interval_months end,0)) mrr from subscriptions s"+
            " where s.status='ACTIVE' and "+scope("s")+" group by s.currency",p));
        LocalDate next=jdbc.query("select min(s.renewal_date) from subscriptions s where s.status in ('ACTIVE','PAST_DUE','EXPIRED') and "+scope("s"),p,
            rs->rs.next() && rs.getDate(1)!=null?rs.getDate(1).toLocalDate():null);
        var currencies=amounts.entrySet().stream().map(entry->{
            var a=entry.getValue();var contracted=value(a,"contracted");var expenses=value(a,"expenses");var cash=value(a,"cash_collected");var mrr=value(a,"mrr");
            return new CurrencySummary(entry.getKey(),contracted,value(a,"invoiced"),value(a,"remaining_to_invoice"),value(a,"collected"),cash,
                value(a,"pending_check_coverage"),value(a,"outstanding_amount"),value(a,"uncovered_outstanding"),value(a,"estimated_cost"),expenses,
                cash.subtract(expenses),contracted.subtract(expenses),mrr,mrr.multiply(BigDecimal.valueOf(12)));
        }).toList();
        return new Summary(currencies,next);
    }
    private String scope(String alias) { return "(:client is null or "+alias+".client_id=:client) and (:project is null or "+alias+".project_id=:project)"; }
    private BigDecimal value(Map<String,BigDecimal> row,String key) { return row.getOrDefault(key,BigDecimal.ZERO); }
    private void add(Map<Currency,Map<String,BigDecimal>> sums,List<Map<String,Object>> rows) {
        for(var row:rows) {
            var values=sums.computeIfAbsent(Currency.valueOf((String)row.get("currency")),k->new HashMap<>());
            row.forEach((key,value)->{if(value instanceof BigDecimal amount) values.merge(key,amount,BigDecimal::add);});
        }
    }
}
