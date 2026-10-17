package com.codevo.businessos.billing.subscription;
import com.codevo.businessos.common.BusinessDates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class SubscriptionInvoiceAutomationJob {
    private static final Logger log=LoggerFactory.getLogger(SubscriptionInvoiceAutomationJob.class);
    private final SubscriptionInvoiceAutomation automation;
    private final SubscriptionService subscriptions;
    private final BusinessDates dates;
    public SubscriptionInvoiceAutomationJob(SubscriptionInvoiceAutomation automation,SubscriptionService subscriptions,BusinessDates dates) { this.automation=automation;this.subscriptions=subscriptions;this.dates=dates; }
    @Scheduled(fixedDelayString="${app.jobs.delay-ms:60000}",initialDelayString="${app.jobs.initial-delay-ms:10000}")
    public void run() {
        for(var id:automation.candidates(dates.today())) {
            try { subscriptions.scheduledInvoice(id); }
            catch(RuntimeException failure) {
                automation.recordFailure(id);
                log.warn("Renewal invoice automation failed for subscription {}; manual review required",id);
            }
        }
    }
}
