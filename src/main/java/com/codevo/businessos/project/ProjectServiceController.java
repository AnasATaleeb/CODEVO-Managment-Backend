package com.codevo.businessos.project;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/project-services")
public class ProjectServiceController {
    private final ProjectServiceApplicationService service;
    private final UninvoicedWorkService uninvoiced;
    public ProjectServiceController(ProjectServiceApplicationService service,UninvoicedWorkService uninvoiced) { this.service=service;this.uninvoiced=uninvoiced; }
    @GetMapping public PageResponse<ProjectServiceDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/uninvoiced") public ProjectServiceDtos.UninvoicedPage uninvoiced(@ModelAttribute ListFilter filter) { return uninvoiced.list(filter); }
    @GetMapping("/{id}") public ProjectServiceDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectServiceDtos.View create(@Valid @RequestBody ProjectServiceDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public ProjectServiceDtos.View update(@PathVariable UUID id,@Valid @RequestBody ProjectServiceDtos.Request request) { return service.update(id,request); }
}
