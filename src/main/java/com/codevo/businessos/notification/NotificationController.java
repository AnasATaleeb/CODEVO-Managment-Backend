package com.codevo.businessos.notification;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping public PageResponse<Map<String,Object>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="false") boolean unread) { return service.list(page,size,unread); }
    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@PathVariable UUID id) { service.read(id); }
    @PostMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT) public void readAll() { service.readAll(); }
}
