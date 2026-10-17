package com.codevo.businessos.project;
import com.codevo.businessos.billing.invoice.InvoiceDtos;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/v1/projects/{projectId}/billing-plan")
public class ProjectBillingPlanController {
    private final ProjectBillingPlanService service;
    public ProjectBillingPlanController(ProjectBillingPlanService service) { this.service=service; }
    @GetMapping public List<ProjectBillingPlanService.View> list(@PathVariable UUID projectId) { return service.list(projectId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProjectBillingPlanService.View create(@PathVariable UUID projectId,@Valid @RequestBody ProjectBillingPlanService.Create r) { return service.create(projectId,r); }
    @PostMapping("/{id}/invoice") public InvoiceDtos.View invoice(@PathVariable UUID projectId,@PathVariable UUID id,@Valid @RequestBody ProjectBillingPlanService.Generate r) { return service.generate(projectId,id,r); }
    @PostMapping("/{id}/cancel") public ProjectBillingPlanService.View cancel(@PathVariable UUID projectId,@PathVariable UUID id,@Valid @RequestBody ProjectBillingPlanService.Cancel r) { return service.cancel(projectId,id,r); }
}
