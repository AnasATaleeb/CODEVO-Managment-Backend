package com.codevo.businessos.task;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service=service; }
    @GetMapping public PageResponse<TaskDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public TaskDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TaskDtos.View create(@Valid @RequestBody TaskDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public TaskDtos.View update(@PathVariable UUID id,@Valid @RequestBody TaskDtos.Request request) { return service.update(id,request); }
}
