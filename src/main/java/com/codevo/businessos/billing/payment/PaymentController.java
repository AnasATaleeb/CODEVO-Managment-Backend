package com.codevo.businessos.billing.payment;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;
@RestController @RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service=service; }
    @GetMapping("/payments") public PageResponse<PaymentDtos.View> list(@ModelAttribute ListFilter f) { return service.list(f); }
    @GetMapping("/payments/{id}") public PaymentDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping("/payments") @ResponseStatus(HttpStatus.CREATED) public PaymentDtos.View create(@Valid @RequestBody PaymentDtos.Request r) { return service.create(r); }
    @PostMapping("/payments/{id}/reverse") public PaymentDtos.View reverse(@PathVariable UUID id,@Valid @RequestBody PaymentDtos.Reason r) { return service.reverse(id,r.reason()); }
    @GetMapping("/checks") public PageResponse<Map<String,Object>> checks(@ModelAttribute CheckFilter f) { return service.checkList(f); }
    @GetMapping("/checks/{id}") public Map<String,Object> check(@PathVariable UUID id) { return service.getCheck(id); }
    @PostMapping("/checks/{id}/status") public PaymentDtos.CheckView transition(@PathVariable UUID id,@Valid @RequestBody PaymentDtos.Transition r) { return service.transition(id,r); }
    @PostMapping("/checks/{id}/reverse") public PaymentDtos.CheckView reverseCheck(@PathVariable UUID id,@Valid @RequestBody PaymentDtos.Reason r) { return service.reverseCheck(id,r.reason()); }
}
