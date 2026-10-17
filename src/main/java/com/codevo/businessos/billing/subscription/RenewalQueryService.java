package com.codevo.businessos.billing.subscription;

import com.codevo.businessos.common.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class RenewalQueryService {
    public record Page(List<Map<String,Object>> content,int page,int size,long totalElements,int totalPages,
        List<Map<String,Object>> totals,LocalDate asOf) {}
    private final NamedParameterJdbcTemplate jdbc;
    private final BusinessDates dates;
    public RenewalQueryService(NamedParameterJdbcTemplate jdbc,BusinessDates dates) { this.jdbc=jdbc;this.dates=dates; }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public Page list(RenewalFilter f) {
        var pageable=f.pageable(Set.of("renewalDate","name","status","createdAt"));LocalDate today=dates.today();
        var p=new MapSqlParameterSource().addValue("client",SqlIds.text(f.getClientId())).addValue("project",SqlIds.text(f.getProjectId()))
            .addValue("subscription",SqlIds.text(f.getSubscriptionId())).addValue("currency",f.getCurrency()==null?null:f.getCurrency().name()).addValue("status",f.getStatus()).addValue("q","%"+ListFilter.escape(f.getQ())+"%")
            .addValue("from",f.getFrom()).addValue("to",f.getTo()).addValue("today",today).addValue("week",today.plusDays(7)).addValue("month",today.plusDays(30))
            .addValue("limit",f.getSize()).addValue("offset",pageable.getOffset());
        if(f.getStatus()!=null && !Set.of("DRAFT","ACTIVE","PAST_DUE","EXPIRED","PAUSED","CANCELLED","ALL").contains(f.getStatus()))
            throw DomainException.invalid("Unsupported subscription status");
        String where=" where (:subscription is null or s.id=:subscription) and (:client is null or s.client_id=:client) and (:project is null or s.project_id=:project)"+
            " and (:currency is null or s.currency=:currency) and (:from is null or s.renewal_date>=:from) and (:to is null or s.renewal_date<=:to)"+
            " and ((:status is null and s.status not in ('DRAFT','CANCELLED')) or :status='ALL' or s.status=:status)"+
            " and (s.name like :q escape '=' or c.display_name like :q escape '=' or pr.name like :q escape '=')";
        String join=" from subscriptions s join clients c on c.id=s.client_id left join projects pr on pr.id=s.project_id"+
            " left join subscription_renewal_invoices r on r.subscription_id=s.id and r.retired_at is null and r.period_start=(select max(r2.period_start) from subscription_renewal_invoices r2 where r2.subscription_id=s.id and r2.retired_at is null)"+
            " left join invoices i on i.id=r.invoice_id";
        long count=jdbc.queryForObject("select count(*)"+join+where,p,Long.class);
        var totals=jdbc.queryForList("select s.currency,count(*) subscriptions,"+
            "sum(case when s.renewal_date=:today and s.status in ('ACTIVE','PAST_DUE','EXPIRED') then 1 else 0 end) dueToday,"+
            "sum(case when s.renewal_date between :today and :week and s.status in ('ACTIVE','PAST_DUE','EXPIRED') then 1 else 0 end) dueIn7Days,"+
            "sum(case when s.renewal_date between :today and :month and s.status in ('ACTIVE','PAST_DUE','EXPIRED') then 1 else 0 end) dueIn30Days,"+
            "sum(case when s.status='EXPIRED' or s.status in ('ACTIVE','PAST_DUE') and date_add(s.current_period_end,interval s.grace_period_days day)<:today then 1 else 0 end) expired,"+
            "sum((select count(*) from subscription_renewal_invoices unpaid_r join invoices unpaid_i on unpaid_i.id=unpaid_r.invoice_id where unpaid_r.subscription_id=s.id and unpaid_r.retired_at is null and unpaid_i.status not in ('DRAFT','VOID','CANCELLED') and unpaid_i.balance_due>0)) unpaidRenewalInvoices,"+
            "sum(case when s.status='ACTIVE' then s.amount/nullif(case s.billing_interval when 'MONTHLY' then 1 when 'QUARTERLY' then 3 when 'SEMI_ANNUAL' then 6 when 'YEARLY' then 12 else s.custom_interval_months end,0) else 0 end) mrr"+
            join+where+" group by s.currency order by s.currency",p);
        totals.forEach(t->t.put("arr",((java.math.BigDecimal)t.get("mrr")).multiply(java.math.BigDecimal.valueOf(12))));
        String sort=switch(f.getSort()) {case "name"->"s.name";case "status"->"s.status";case "createdAt"->"s.created_at";default->"s.renewal_date";};
        var rows=jdbc.queryForList("select s.id,s.client_id clientId,s.project_id projectId,s.project_service_id projectServiceId,s.name,s.status,"+
            "s.billing_interval billingInterval,s.amount,s.currency,s.start_date startDate,s.current_period_start currentPeriodStart,s.current_period_end currentPeriodEnd,"+
            "s.renewal_date renewalDate,s.auto_renew autoRenew,s.grace_period_days gracePeriodDays,s.custom_interval_months customIntervalMonths,s.version,s.invoice_automation_enabled invoiceAutomationEnabled,s.invoice_automation_error invoiceAutomationError,"+
            "c.display_name clientName,pr.name projectName,i.id invoiceId,i.invoice_number invoiceNumber,i.status invoiceStatus,i.total invoiceTotal,"+
            "i.paid_amount collectedAmount,i.pending_check_amount pendingCheckCoverage,i.balance_due outstandingAmount,"+
            "greatest(i.balance_due-i.pending_check_amount,0) uncoveredOutstanding,r.period_start invoicePeriodStart,r.period_end invoicePeriodEnd"+
            join+where+" order by "+sort+" "+f.getDirection()+",s.id limit :limit offset :offset",p);
        // JDBC date values otherwise serialize as instants, which cannot populate date-only inputs.
        rows.forEach(row -> row.replaceAll((key,value) -> value instanceof java.sql.Date day ? day.toLocalDate() : value));
        return new Page(rows,f.getPage(),f.getSize(),count,(int)((count+f.getSize()-1)/f.getSize()),totals,today);
    }
}
