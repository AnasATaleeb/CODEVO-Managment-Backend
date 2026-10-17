package com.codevo.businessos.servicecatalog;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceCatalogItemController {
    private final ServiceCatalogItemService service;
    public ServiceCatalogItemController(ServiceCatalogItemService service) { this.service=service; }
    @GetMapping public PageResponse<ServiceCatalogItemDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public ServiceCatalogItemDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ServiceCatalogItemDtos.View create(@Valid @RequestBody ServiceCatalogItemDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public ServiceCatalogItemDtos.View update(@PathVariable UUID id,@Valid @RequestBody ServiceCatalogItemDtos.Request request) { return service.update(id,request); }
}
