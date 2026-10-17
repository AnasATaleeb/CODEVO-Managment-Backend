package com.codevo.businessos.billing.expense;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.codevo.businessos.common.*;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {
    private final ExpenseService service;
    public ExpenseController(ExpenseService service) { this.service=service; }
    @GetMapping public PageResponse<ExpenseDtos.View> list(@ModelAttribute ListFilter filter) { return service.list(filter); }
    @GetMapping("/{id}") public ExpenseDtos.View get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ExpenseDtos.View create(@Valid @RequestBody ExpenseDtos.Request request) { return service.create(request); }
    @RequestMapping(value="/{id}",method={RequestMethod.PATCH,RequestMethod.PUT})
    public ExpenseDtos.View update(@PathVariable UUID id,@Valid @RequestBody ExpenseDtos.Request request) { return service.update(id,request); }
}
