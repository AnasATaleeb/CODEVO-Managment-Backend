package com.codevo.businessos.settings;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/settings")
public class SettingsController {
    private final SettingsService service;
    public SettingsController(SettingsService service) { this.service=service; }
    @GetMapping public Map<String,Object> get() { return service.get(); }
    @PutMapping public Map<String,Object> update(@Valid @RequestBody SettingsService.Request r) { return service.update(r); }
}
