package com.codevo.businessos.project;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectApplicationService service;
    public ProjectController(ProjectApplicationService service) { this.service=service; }
    @GetMapping public PageResponse<ProjectDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public ProjectDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectDtos.View create(@Valid @RequestBody ProjectDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public ProjectDtos.View update(@PathVariable UUID id,@Valid @RequestBody ProjectDtos.Request request) { return service.update(id,request); }
}
