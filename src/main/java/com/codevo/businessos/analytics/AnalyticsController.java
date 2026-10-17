package com.codevo.businessos.analytics;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService service;
    public AnalyticsController(AnalyticsService service) { this.service=service; }
    @GetMapping({"/revenue","/profit","/expenses"}) public Map<String,Object> financial(@ModelAttribute AnalyticsFilter f) { return service.financial(f); }
    @GetMapping("/clients") public Map<String,Object> clients(@ModelAttribute AnalyticsFilter f) { return Map.of("counts",service.clientCounts(f),"contributions",service.clientProfit(f)); }
    @GetMapping("/projects") public List<Map<String,Object>> projects(@ModelAttribute AnalyticsFilter f) { return service.projectProfit(f); }
    @GetMapping("/sales") public Map<String,Object> sales(@ModelAttribute AnalyticsFilter f) { return service.sales(f); }
}
