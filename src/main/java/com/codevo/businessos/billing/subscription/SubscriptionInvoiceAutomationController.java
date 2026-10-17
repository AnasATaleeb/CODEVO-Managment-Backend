package com.codevo.businessos.billing.subscription;
import com.codevo.businessos.billing.invoice.InvoiceDtos;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/subscriptions/{id}/invoice-automation")
public class SubscriptionInvoiceAutomationController {
    private final SubscriptionInvoiceAutomation automation;
    public SubscriptionInvoiceAutomationController(SubscriptionInvoiceAutomation automation) { this.automation=automation; }
    @GetMapping public SubscriptionInvoiceAutomation.Policy policy(@PathVariable UUID id) { return automation.policy(id); }
    @PutMapping public SubscriptionInvoiceAutomation.Policy configure(@PathVariable UUID id,@Valid @RequestBody SubscriptionInvoiceAutomation.Configure request) { return automation.configure(id,request); }
    @PostMapping("/run") public InvoiceDtos.View run(@PathVariable UUID id) { return automation.runNow(id); }
}
