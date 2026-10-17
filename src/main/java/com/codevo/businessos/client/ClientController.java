package com.codevo.businessos.client;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientService service;
    public ClientController(ClientService service) { this.service=service; }
    @GetMapping public PageResponse<ClientDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public ClientDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ClientDtos.View create(@Valid @RequestBody ClientDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public ClientDtos.View update(@PathVariable UUID id,@Valid @RequestBody ClientDtos.Request request) { return service.update(id,request); }
}
