package com.codevo.businessos.user;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service=service; }
    @GetMapping public List<UserService.View> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserService.View create(@Valid @RequestBody UserService.Request r) { return service.create(r); }
    @PostMapping("/{id}/disable") @ResponseStatus(HttpStatus.NO_CONTENT) public void disable(@PathVariable UUID id, @Valid @RequestBody com.codevo.businessos.billing.invoice.InvoiceDtos.Reason reason) { service.disable(id,reason.reason()); }
}
