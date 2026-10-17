package com.codevo.businessos.billing.subscription;

import com.codevo.businessos.activity.ActivityService;
import com.codevo.businessos.billing.invoice.InvoiceDtos;
import com.codevo.businessos.common.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class SubscriptionInvoiceAutomation {
    public record Configure(@NotNull Long version,boolean enabled,@Min(0) @Max(90) int leadDays,@Min(0) @Max(90) int dueDays) {}
    public record Policy(UUID subscriptionId,long version,boolean enabled,int leadDays,int dueDays,String lastError) {}
    private final SubscriptionRepository subscriptions;
    private final SubscriptionService service;
    private final JdbcTemplate jdbc;
    private final ActivityService activity;
    public SubscriptionInvoiceAutomation(SubscriptionRepository subscriptions,SubscriptionService service,JdbcTemplate jdbc,ActivityService activity) {
        this.subscriptions=subscriptions;this.service=service;this.jdbc=jdbc;this.activity=activity;
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public Policy policy(UUID id) { return view(subscriptions.findById(id).orElseThrow(()->DomainException.missing("Subscription"))); }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public Policy configure(UUID id,Configure request) {
        var subscription=subscriptions.lockById(id).orElseThrow(()->DomainException.missing("Subscription"));
        if(subscription.getVersion()!=request.version())throw DomainException.conflict("Subscription changed; refresh automation settings");
        if(request.enabled()&&Set.of("DRAFT","PAUSED","CANCELLED").contains(subscription.getStatus()))throw DomainException.conflict("Activate the subscription before enabling invoice automation");
        subscription.setInvoiceAutomationEnabled(request.enabled());subscription.setInvoiceLeadDays(request.leadDays());subscription.setInvoiceDueDays(request.dueDays());subscription.setInvoiceAutomationError(null);
        subscriptions.flush();
        activity.record("subscriptions",id,subscription.getClientId(),"INVOICE_AUTOMATION_CONFIGURED","Automatic renewal invoices "+(request.enabled()?"enabled":"disabled")+"; lead days "+request.leadDays()+", due days "+request.dueDays()+". Payment is entered manually.");
        return view(subscription);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View runNow(UUID id) { return service.scheduledInvoice(id); }
    /** Bounded scheduler query, restricted to explicit opt-in. Creation serializes on each subscription row. */
    public List<UUID> candidates(java.time.LocalDate today) {
        return jdbc.query("select id from subscriptions where invoice_automation_enabled=true and invoice_automation_error is null and status in ('ACTIVE','PAST_DUE','EXPIRED') and date_sub(renewal_date,interval invoice_lead_days day)<=? and not exists(select 1 from subscription_renewal_invoices link where link.subscription_id=subscriptions.id and link.retired_at is null and link.period_start=date_add(subscriptions.current_period_end,interval 1 day)) order by renewal_date,id limit 100",(rs,n)->SqlIds.uuid(rs,1),today);
    }
    @Transactional
    public void recordFailure(UUID id) { jdbc.update("update subscriptions set invoice_automation_error='Automatic invoice generation failed; review subscription terms and generate manually.' where id=?",id); }
    private Policy view(Subscription s) { return new Policy(s.getId(),s.getVersion(),s.isInvoiceAutomationEnabled(),s.getInvoiceLeadDays(),s.getInvoiceDueDays(),s.getInvoiceAutomationError()); }
}
