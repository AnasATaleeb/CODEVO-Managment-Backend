package com.codevo.businessos.billing.subscription;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.codevo.businessos.billing.invoice.InvoiceDtos;
import com.codevo.businessos.billing.invoice.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.activity.ActivityService;

@Service
@Transactional(readOnly=true)
public class SubscriptionService {
    private final SubscriptionRepository repo;
    private final SubscriptionMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock; private final BusinessDates dates;
    private final JdbcTemplate jdbc;
    private final InvoiceService invoices;
    public SubscriptionService(SubscriptionRepository repo, SubscriptionMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock, BusinessDates dates,JdbcTemplate jdbc,InvoiceService invoices) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock; this.dates=dates;this.jdbc=jdbc;this.invoices=invoices;
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PageResponse<SubscriptionDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("name","status",Set.of("clientId","projectId"),"renewalDate"),filter.pageable(Set.of("createdAt","name","status","renewalDate","clientId","projectId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public SubscriptionDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("Subscription"))); }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public SubscriptionDtos.View create(SubscriptionDtos.Request request) {
        var e=new Subscription(); mapper.apply(request,e); requireUniqueLink(e);validate(e); repo.saveAndFlush(e);
        activity.record("subscriptions",e.getId(),e.getClientId(),"CREATED","Created Subscription: "+e.getName());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public SubscriptionDtos.View update(UUID id, SubscriptionDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Subscription"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        String before=e.getStatus()+" / "+e.getAmount()+" "+e.getCurrency()+" / "+e.getBillingInterval()+" / period end "+e.getCurrentPeriodEnd();
        UUID previousService=e.getProjectServiceId();String previousStatus=e.getStatus();
        mapper.apply(request,e);
        if(!java.util.Objects.equals(previousService,e.getProjectServiceId()) || "CANCELLED".equals(previousStatus) && !"CANCELLED".equals(e.getStatus())) requireUniqueLink(e);
        validate(e); repo.flush();
        activity.record("subscriptions",e.getId(),e.getClientId(),"UPDATED","Updated Subscription: "+before+" → "+e.getStatus()+" / "+e.getAmount()+" "+e.getCurrency()+" / "+e.getBillingInterval()+" / period end "+e.getCurrentPeriodEnd());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public SubscriptionDtos.View createFromService(SubscriptionDtos.FromService r) {
        lockService(r.projectServiceId());
        Map<String,Object> source=jdbc.queryForMap("select ps.*,pc.agreed_amount,p.client_id from project_services ps join project_service_commercial pc on pc.id=ps.id join projects p on p.id=ps.project_id where ps.id=?",r.projectServiceId());
        String interval=(String)source.get("billing_interval");
        if("CANCELLED".equals(source.get("status")) || interval==null || !Set.of("MONTHLY","QUARTERLY","SEMI_ANNUAL","YEARLY","CUSTOM").contains(interval))
            throw DomainException.invalid("Choose a recurring project service with a billing interval");
        Subscription e=new Subscription();e.setProjectServiceId(r.projectServiceId());e.setProjectId(UUID.fromString((String)source.get("project_id")));
        e.setClientId(UUID.fromString((String)source.get("client_id")));e.setName((String)source.get("custom_name"));
        e.setAmount((BigDecimal)source.get("agreed_amount"));e.setCurrency(Currency.valueOf((String)source.get("currency")));
        e.setBillingInterval(interval);e.setCustomIntervalMonths((Integer)source.get("custom_interval_months"));
        LocalDate start=r.startDate()!=null?r.startDate():source.get("start_date") instanceof java.sql.Date d?d.toLocalDate():dates.today();
        var period=SubscriptionPolicy.renew(start.minusDays(1),interval,e.getCustomIntervalMonths());
        e.setStartDate(start);e.setCurrentPeriodStart(period.start());e.setCurrentPeriodEnd(period.end());e.setRenewalDate(period.renewal());
        e.setStatus(r.status()==null?"ACTIVE":r.status());e.setAutoRenew(Boolean.TRUE.equals(r.autoRenew()));e.setGracePeriodDays(r.gracePeriodDays()==null?0:r.gracePeriodDays());
        requireUniqueLink(e);validate(e);repo.saveAndFlush(e);
        activity.record("subscriptions",e.getId(),e.getClientId(),"CREATED","Created subscription from project service: "+e.getName());
        return mapper.view(e);
    }
    /** Invoice generation and period advancement are separate, explicit financial actions. */
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View renewalInvoice(UUID id,SubscriptionDtos.RenewalInvoice r) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Subscription"));
        if(r.version()!=e.getVersion()) throw DomainException.conflict("Subscription changed; refresh before invoicing");
        if(Set.of("DRAFT","CANCELLED","PAUSED").contains(e.getStatus())) throw DomainException.conflict("Activate the subscription before generating a renewal invoice");
        return generateInvoice(e,r,false);
    }
    /** Trusted scheduler entry point: no HTTP mapping, and all eligibility is checked from locked database state. */
    @Transactional
    public InvoiceDtos.View scheduledInvoice(UUID id) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Subscription"));
        if(!e.isInvoiceAutomationEnabled() || !Set.of("ACTIVE","PAST_DUE","EXPIRED").contains(e.getStatus()) || e.getRenewalDate().minusDays(e.getInvoiceLeadDays()).isAfter(dates.today()))return null;
        LocalDate due=e.getRenewalDate().plusDays(e.getInvoiceDueDays());
        if(due.isBefore(dates.today()))due=dates.today();
        var invoice=generateInvoice(e,new SubscriptionDtos.RenewalInvoice(e.getVersion(),dates.today(),due,"ISSUED"),true);
        e.setInvoiceAutomationError(null);repo.flush();
        return invoice;
    }
    private InvoiceDtos.View generateInvoice(Subscription e,SubscriptionDtos.RenewalInvoice r,boolean scheduled) {
        var period=SubscriptionPolicy.renew(e.getCurrentPeriodEnd(),e.getBillingInterval(),e.getCustomIntervalMonths());
        UUID existing=jdbc.query("select invoice_id from subscription_renewal_invoices where subscription_id=? and period_start=? and retired_at is null",
            rs->rs.next()?SqlIds.uuid(rs,1):null,e.getId(),period.start());
        if(existing!=null) {
            var previous=scheduled?invoices.scheduledRenewalSnapshot(existing):invoices.get(existing);
            if(scheduled || !Set.of("VOID","CANCELLED").contains(previous.status())) return previous;
            // The former invoice and period link remain available for audit. Only the current generation is retired.
            jdbc.update("update subscription_renewal_invoices set retired_at=? where invoice_id=? and retired_at is null",java.sql.Timestamp.from(clock.instant()),existing);
        }
        var request=new InvoiceDtos.Request(e.getClientId(),e.getProjectId(),r.status()==null?"ISSUED":r.status(),r.issueDate(),r.dueDate(),
            e.getCurrency(),BigDecimal.ZERO,BigDecimal.ZERO,"Renewal period "+period.start()+" — "+period.end(),
            List.of(new InvoiceDtos.ItemRequest(e.getProjectServiceId(),e.getName()+" — "+period.start()+" to "+period.end(),BigDecimal.ONE,e.getAmount())));
        var invoice=scheduled?invoices.createScheduledRenewalInvoice(request):invoices.createRenewalInvoice(request);
        jdbc.update("insert into subscription_renewal_invoices(id,subscription_id,invoice_id,period_start,period_end,created_at) values(?,?,?,?,?,?)",
            UUID.randomUUID(),e.getId(),invoice.id(),period.start(),period.end(),java.sql.Timestamp.from(clock.instant()));
        activity.record("subscriptions",e.getId(),e.getClientId(),"RENEWAL_INVOICED","Generated "+invoice.invoiceNumber()+" for "+period.start()+" through "+period.end()+"; payment and renewal remain separate");
        return invoice;
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public SubscriptionDtos.View renew(UUID id,SubscriptionDtos.Renewal r) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Subscription"));
        if(r.version()!=e.getVersion()) throw DomainException.conflict("Subscription changed; refresh before renewing");
        if(Set.of("DRAFT","CANCELLED","PAUSED").contains(e.getStatus())) throw DomainException.conflict("Activate a valid subscription before renewal");
        var period=SubscriptionPolicy.renew(e.getCurrentPeriodEnd(),e.getBillingInterval(),e.getCustomIntervalMonths());
        e.setCurrentPeriodStart(period.start());e.setCurrentPeriodEnd(period.end());e.setRenewalDate(period.renewal());e.setStatus("ACTIVE");
        validate(e);repo.flush();
        activity.record("subscriptions",id,e.getClientId(),"RENEWED","Renewed "+e.getName()+" through "+period.end()+": "+r.reason());
        return mapper.view(e);
    }
    private void validate(Subscription e) {
        if(e.getProjectServiceId()!=null && !"CANCELLED".equals(e.getStatus())) {
            lockService(e.getProjectServiceId());
        }
        if(e.getClientId()!=null) refs.client(e.getClientId()); refs.project(e.getProjectId(),e.getClientId()); if(e.getProjectServiceId()!=null && e.getProjectId()==null) throw DomainException.invalid("Project service requires a project"); refs.projectService(e.getProjectServiceId(),e.getProjectId()); if(e.getCurrentPeriodEnd().isBefore(e.getCurrentPeriodStart()) || e.getCurrentPeriodStart().isBefore(e.getStartDate())) throw DomainException.invalid("Invalid subscription period"); Money.monthly(e.getAmount(),e.getBillingInterval(),e.getCustomIntervalMonths()); e.setAmount(Money.amount(e.getAmount(),e.getCurrency())); if(e.getStatus().equals("ACTIVE") && e.getCurrentPeriodEnd().plusDays(e.getGracePeriodDays()).isBefore(dates.today())) e.setStatus("EXPIRED"); e.setCancelledAt(e.getStatus().equals("CANCELLED") ? clock.instant() : null);
    }
    private void requireUniqueLink(Subscription e) {
        if(e.getProjectServiceId()==null || "CANCELLED".equals(e.getStatus())) return;
        lockService(e.getProjectServiceId());
        if(repo.existsByProjectServiceIdAndStatusNotAndIdNot(e.getProjectServiceId(),"CANCELLED",e.getId()))
            throw DomainException.conflict("This project service already has a subscription");
    }
    private void lockService(UUID id) {
        if(jdbc.query("select id from project_services where id=? for update",rs->rs.next()?SqlIds.uuid(rs,1):null,id)==null)
            throw DomainException.missing("Project service");
    }
}
