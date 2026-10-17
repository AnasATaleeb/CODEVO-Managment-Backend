package com.codevo.businessos.billing.subscription;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;
import com.codevo.businessos.billing.invoice.InvoiceDtos;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {
    private final SubscriptionService service;
    private final RenewalQueryService renewals;
    public SubscriptionController(SubscriptionService service,RenewalQueryService renewals) { this.service=service;this.renewals=renewals; }
    @GetMapping public PageResponse<SubscriptionDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public SubscriptionDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @GetMapping("/renewals") public RenewalQueryService.Page renewals(@ModelAttribute RenewalFilter f) { return renewals.list(f); }
    @PostMapping("/from-service") @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDtos.View fromService(@Valid @RequestBody SubscriptionDtos.FromService r) { return service.createFromService(r); }
    @PostMapping("/{id}/renewal-invoice")
    public InvoiceDtos.View renewalInvoice(@PathVariable UUID id,@Valid @RequestBody SubscriptionDtos.RenewalInvoice r) { return service.renewalInvoice(id,r); }
    @PostMapping("/{id}/renew") public SubscriptionDtos.View renew(@PathVariable UUID id,@Valid @RequestBody SubscriptionDtos.Renewal r) { return service.renew(id,r); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public SubscriptionDtos.View create(@Valid @RequestBody SubscriptionDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public SubscriptionDtos.View update(@PathVariable UUID id,@Valid @RequestBody SubscriptionDtos.Request request) { return service.update(id,request); }
}
