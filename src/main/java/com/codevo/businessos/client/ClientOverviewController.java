package com.codevo.businessos.client;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.codevo.businessos.security.Permissions;
@RestController @RequestMapping("/api/v1/clients")
public class ClientOverviewController {
    private final ClientOverviewService service;
    public ClientOverviewController(ClientOverviewService service) { this.service=service; }
    @GetMapping("/metrics") public List<Map<String,Object>> metrics(@RequestParam List<UUID> ids,Authentication a) { return service.metrics(ids,Permissions.finance(a)); }
    @GetMapping("/{id}/summary") public Map<String,Object> summary(@PathVariable UUID id,Authentication a) { return service.summary(id,Permissions.finance(a)); }
}
