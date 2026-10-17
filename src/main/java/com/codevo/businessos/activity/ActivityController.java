package com.codevo.businessos.activity;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.codevo.businessos.security.Permissions;
import com.codevo.businessos.common.*;
@RestController @RequestMapping("/api/v1/activity")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service=service; }
    @GetMapping @PreAuthorize("hasAuthority('CRM_READ')")
    public PageResponse<Map<String,Object>> list(@RequestParam(required=false) UUID clientId,@RequestParam(required=false) UUID entityId,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,Authentication auth) {
        return service.list(clientId,entityId,page,size,Permissions.finance(auth));
    }
}
