package com.codevo.businessos.analytics;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.codevo.businessos.security.Permissions;
import com.codevo.businessos.notification.*;
@RestController @RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;private final AttentionService attention;
    public DashboardController(DashboardService service,AttentionService attention) { this.service=service;this.attention=attention; }
    @GetMapping("/summary") public Map<String,Object> summary(@ModelAttribute AnalyticsFilter f,Authentication auth) { return service.summary(f,Permissions.finance(auth),auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("CRM_WRITE"))); }
    @GetMapping("/attention") public List<AttentionService.Item> attention(Authentication auth) {
        boolean sales=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("CRM_WRITE"));
        return attention.attention(Permissions.finance(auth)).stream().filter(i->sales || !i.entityType().equals("leads")).toList();
    }
}
