package com.codevo.businessos.billing.subscription;

import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-18T00:38:43+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class SubscriptionMapperImpl implements SubscriptionMapper {

    @Override
    public SubscriptionDtos.View view(Subscription entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID clientId = null;
        UUID projectId = null;
        UUID projectServiceId = null;
        String name = null;
        String status = null;
        String billingInterval = null;
        BigDecimal amount = null;
        Currency currency = null;
        LocalDate startDate = null;
        LocalDate currentPeriodStart = null;
        LocalDate currentPeriodEnd = null;
        LocalDate renewalDate = null;
        Boolean autoRenew = null;
        Integer gracePeriodDays = null;
        Integer customIntervalMonths = null;
        Instant cancelledAt = null;
        String notes = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;
        boolean invoiceAutomationEnabled = false;
        int invoiceLeadDays = 0;
        int invoiceDueDays = 0;
        String invoiceAutomationError = null;

        id = entity.getId();
        clientId = entity.getClientId();
        projectId = entity.getProjectId();
        projectServiceId = entity.getProjectServiceId();
        name = entity.getName();
        status = entity.getStatus();
        billingInterval = entity.getBillingInterval();
        amount = entity.getAmount();
        currency = entity.getCurrency();
        startDate = entity.getStartDate();
        currentPeriodStart = entity.getCurrentPeriodStart();
        currentPeriodEnd = entity.getCurrentPeriodEnd();
        renewalDate = entity.getRenewalDate();
        autoRenew = entity.getAutoRenew();
        gracePeriodDays = entity.getGracePeriodDays();
        customIntervalMonths = entity.getCustomIntervalMonths();
        cancelledAt = entity.getCancelledAt();
        notes = entity.getNotes();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();
        invoiceAutomationEnabled = entity.isInvoiceAutomationEnabled();
        invoiceLeadDays = entity.getInvoiceLeadDays();
        invoiceDueDays = entity.getInvoiceDueDays();
        invoiceAutomationError = entity.getInvoiceAutomationError();

        SubscriptionDtos.View view = new SubscriptionDtos.View( id, clientId, projectId, projectServiceId, name, status, billingInterval, amount, currency, startDate, currentPeriodStart, currentPeriodEnd, renewalDate, autoRenew, gracePeriodDays, customIntervalMonths, cancelledAt, notes, createdAt, updatedAt, version, invoiceAutomationEnabled, invoiceLeadDays, invoiceDueDays, invoiceAutomationError );

        return view;
    }

    @Override
    public void apply(SubscriptionDtos.Request request, Subscription entity) {
        if ( request == null ) {
            return;
        }

        entity.setClientId( request.clientId() );
        entity.setProjectId( request.projectId() );
        entity.setProjectServiceId( request.projectServiceId() );
        entity.setName( request.name() );
        entity.setStatus( request.status() );
        entity.setBillingInterval( request.billingInterval() );
        entity.setAmount( request.amount() );
        entity.setCurrency( request.currency() );
        entity.setStartDate( request.startDate() );
        entity.setCurrentPeriodStart( request.currentPeriodStart() );
        entity.setCurrentPeriodEnd( request.currentPeriodEnd() );
        entity.setRenewalDate( request.renewalDate() );
        entity.setAutoRenew( request.autoRenew() );
        entity.setGracePeriodDays( request.gracePeriodDays() );
        entity.setCustomIntervalMonths( request.customIntervalMonths() );
        entity.setCancelledAt( request.cancelledAt() );
        entity.setNotes( request.notes() );
    }
}
