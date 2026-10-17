package com.codevo.businessos.common;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/references")
public class ReferenceLookupController {
    private final ReferenceLookupService service;
    public ReferenceLookupController(ReferenceLookupService service) { this.service=service; }
    @GetMapping public Map<String,String> names(@RequestParam String type,@RequestParam List<UUID> ids) { return service.names(type,ids); }
}
