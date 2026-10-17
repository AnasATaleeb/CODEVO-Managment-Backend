package com.codevo.businessos.billing.expense;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.activity.ActivityService;

@Service
@Transactional(readOnly=true)
public class ExpenseService {
    private final ExpenseRepository repo;
    private final ExpenseMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public ExpenseService(ExpenseRepository repo, ExpenseMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PageResponse<ExpenseDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("description",null,Set.of("clientId","projectId"),"expenseDate"),filter.pageable(Set.of("createdAt","description","expenseDate","clientId","projectId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ExpenseDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("Expense"))); }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ExpenseDtos.View create(ExpenseDtos.Request request) {
        var e=new Expense(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("expenses",e.getId(),e.getClientId(),"CREATED","Created Expense: "+e.getDescription());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ExpenseDtos.View update(UUID id, ExpenseDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Expense"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        String before=e.getAmount()+" "+e.getCurrency()+" on "+e.getExpenseDate();
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("expenses",e.getId(),e.getClientId(),"UPDATED","Updated Expense: "+before+" → "+e.getAmount()+" "+e.getCurrency()+" on "+e.getExpenseDate());
        return mapper.view(e);
    }
    private void validate(Expense e) {
        if(e.getClientId()!=null) refs.client(e.getClientId()); refs.project(e.getProjectId(),e.getClientId()); e.setAmount(Money.amount(e.getAmount(),e.getCurrency())); if(e.getAmount().signum()<=0) throw DomainException.invalid("Expense must be positive"); if(e.getRecurring() && e.getBillingInterval()==null) throw DomainException.invalid("Recurring expenses need a billing interval");
    }
}
