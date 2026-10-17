package com.codevo.businessos.lead;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/leads")
public class LeadConversionController {
    private final LeadConversionService service;
    public LeadConversionController(LeadConversionService service) { this.service=service; }
    @PostMapping("/{id}/convert") public LeadConversionService.Result convert(@PathVariable UUID id,@Valid @RequestBody LeadConversionService.Request request) { return service.convert(id,request); }
}
