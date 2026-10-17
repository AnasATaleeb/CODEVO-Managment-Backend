package com.codevo.businessos.asset;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service) { this.service=service; }
    @GetMapping public PageResponse<AssetDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public AssetDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public AssetDtos.View create(@Valid @RequestBody AssetDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public AssetDtos.View update(@PathVariable UUID id,@Valid @RequestBody AssetDtos.Request request) { return service.update(id,request); }
}
