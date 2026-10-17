package com.codevo.businessos.billing.expense;

import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-17T20:33:30+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class ExpenseMapperImpl implements ExpenseMapper {

    @Override
    public ExpenseDtos.View view(Expense entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID clientId = null;
        UUID projectId = null;
        String category = null;
        String description = null;
        String vendor = null;
        BigDecimal amount = null;
        Currency currency = null;
        LocalDate expenseDate = null;
        Boolean recurring = null;
        String billingInterval = null;
        String notes = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;

        id = entity.getId();
        clientId = entity.getClientId();
        projectId = entity.getProjectId();
        category = entity.getCategory();
        description = entity.getDescription();
        vendor = entity.getVendor();
        amount = entity.getAmount();
        currency = entity.getCurrency();
        expenseDate = entity.getExpenseDate();
        recurring = entity.getRecurring();
        billingInterval = entity.getBillingInterval();
        notes = entity.getNotes();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();

        ExpenseDtos.View view = new ExpenseDtos.View( id, clientId, projectId, category, description, vendor, amount, currency, expenseDate, recurring, billingInterval, notes, createdAt, updatedAt, version );

        return view;
    }

    @Override
    public void apply(ExpenseDtos.Request request, Expense entity) {
        if ( request == null ) {
            return;
        }

        entity.setClientId( request.clientId() );
        entity.setProjectId( request.projectId() );
        entity.setCategory( request.category() );
        entity.setDescription( request.description() );
        entity.setVendor( request.vendor() );
        entity.setAmount( request.amount() );
        entity.setCurrency( request.currency() );
        entity.setExpenseDate( request.expenseDate() );
        entity.setRecurring( request.recurring() );
        entity.setBillingInterval( request.billingInterval() );
        entity.setNotes( request.notes() );
    }
}
