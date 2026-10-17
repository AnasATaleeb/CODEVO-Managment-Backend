package com.codevo.businessos.billing;

import com.codevo.businessos.common.ReferenceChecks;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class FinancialSummaryController {
    private final FinancialSummaryService service;
    private final ReferenceChecks refs;
    public FinancialSummaryController(FinancialSummaryService service,ReferenceChecks refs) { this.service=service;this.refs=refs; }
    @GetMapping("/projects/{id}/financial-summary") @PreAuthorize("hasAuthority('FINANCE_READ')")
    public FinancialSummaryService.Summary project(@PathVariable UUID id) { refs.project(id,null);return service.summary(null,id); }
    @GetMapping("/clients/{id}/financial-summary") @PreAuthorize("hasAuthority('FINANCE_READ')")
    public FinancialSummaryService.Summary client(@PathVariable UUID id) { refs.client(id);return service.summary(id,null); }
}
