package com.codevo.businessos.project;

import com.codevo.businessos.billing.invoice.InvoiceDtos;
import com.codevo.businessos.billing.invoice.InvoiceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectInvoiceController {
    private final InvoiceService invoices;
    public ProjectInvoiceController(InvoiceService invoices) { this.invoices=invoices; }
    @PostMapping("/{id}/invoices") @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDtos.View create(@PathVariable UUID id,@Valid @RequestBody InvoiceDtos.FromProjectRequest request) {
        return invoices.createFromProject(id,request);
    }
}
