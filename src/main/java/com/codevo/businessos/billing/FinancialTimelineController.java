package com.codevo.businessos.billing;

import com.codevo.businessos.common.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/financial-timeline")
public class FinancialTimelineController {
    private final FinancialTimelineService service;
    public FinancialTimelineController(FinancialTimelineService service) { this.service=service; }
    @GetMapping public PageResponse<Map<String,Object>> list(@RequestParam(required=false) UUID invoiceId,@ModelAttribute ListFilter f) {
        return service.list(invoiceId,f);
    }
}
