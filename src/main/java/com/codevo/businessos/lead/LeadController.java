package com.codevo.businessos.lead;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {
    private final LeadService service;
    public LeadController(LeadService service) { this.service=service; }
    @GetMapping public PageResponse<LeadDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public LeadDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public LeadDtos.View create(@Valid @RequestBody LeadDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public LeadDtos.View update(@PathVariable UUID id,@Valid @RequestBody LeadDtos.Request request) { return service.update(id,request); }
}
