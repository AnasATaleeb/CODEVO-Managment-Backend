package com.codevo.businessos.receivables;

import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/receivables")
public class ReceivableController {
    private final ReceivableService service;
    public ReceivableController(ReceivableService service) { this.service=service; }
    @GetMapping public ReceivableDtos.Page list(@ModelAttribute ReceivableFilter filter) { return service.list(filter); }
}
