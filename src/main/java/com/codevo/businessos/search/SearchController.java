package com.codevo.businessos.search;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.codevo.businessos.security.Permissions;
@RestController @RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService service;
    public SearchController(SearchService service) { this.service=service; }
    @GetMapping public List<SearchService.Result> search(@RequestParam(defaultValue="") String q,Authentication a) {
        return service.search(q,Permissions.finance(a),a.getAuthorities().stream().anyMatch(p->p.getAuthority().equals("CRM_WRITE")));
    }
}
