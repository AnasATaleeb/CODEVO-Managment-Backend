package com.codevo.businessos.billing.invoice;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;
@RestController @RequestMapping("/api/v1/invoices")
public class InvoiceController {
    private final InvoiceService service;
    public InvoiceController(InvoiceService service) { this.service=service; }
    @GetMapping public PageResponse<InvoiceDtos.View> list(@ModelAttribute ListFilter f) { return service.list(f); }
    @GetMapping("/{id}") public InvoiceDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public InvoiceDtos.View create(@Valid @RequestBody InvoiceDtos.Request r) { return service.create(r); }
    @PostMapping("/{id}/issue") public InvoiceDtos.View issue(@PathVariable UUID id) { return service.issue(id); }
    @PostMapping("/{id}/void") public InvoiceDtos.View voidInvoice(@PathVariable UUID id,@Valid @RequestBody InvoiceDtos.Reason r) { return service.voidInvoice(id,r.reason()); }
}
