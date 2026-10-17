package com.codevo.businessos.client;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/notes")
public class ClientNoteController {
    private final ClientNoteService service;
    public ClientNoteController(ClientNoteService service) { this.service=service; }
    @GetMapping public PageResponse<ClientNoteDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public ClientNoteDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ClientNoteDtos.View create(@Valid @RequestBody ClientNoteDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public ClientNoteDtos.View update(@PathVariable UUID id,@Valid @RequestBody ClientNoteDtos.Request request) { return service.update(id,request); }
}
