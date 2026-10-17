package com.codevo.businessos.calendar;
import java.util.*;
import java.time.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.codevo.businessos.security.Permissions;
@RestController @RequestMapping("/api/v1/calendar")
public class CalendarQueryController {
    private final CalendarQueryService service;
    public CalendarQueryController(CalendarQueryService service) { this.service=service; }
    @GetMapping public List<Map<String,Object>> events(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) String type,
        @RequestParam(required=false) UUID clientId,@RequestParam(required=false) UUID projectId,@RequestParam(required=false) UUID assignedUserId,Authentication a) {
        return service.events(from,to,type,clientId,projectId,assignedUserId,Permissions.finance(a),a.getAuthorities().stream().anyMatch(p->p.getAuthority().equals("CRM_WRITE")));
    }
}
