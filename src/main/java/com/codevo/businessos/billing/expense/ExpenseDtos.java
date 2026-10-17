package com.codevo.businessos.billing.expense;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class ExpenseDtos {
    private ExpenseDtos() {}
    public record Request( UUID clientId,
         UUID projectId,
        @NotBlank @Size(max=255) String category,
        @NotBlank @Size(max=255) String description,
        @Size(max=255) String vendor,
        @NotNull @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal amount,
        @NotNull Currency currency,
        @NotNull LocalDate expenseDate,
        @NotNull Boolean recurring,
        @Pattern(regexp="MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|CUSTOM") String billingInterval,
        @Size(max=10000) String notes, Long version) {}
    public record View(UUID id, UUID clientId, UUID projectId, String category, String description, String vendor, BigDecimal amount, Currency currency, LocalDate expenseDate, Boolean recurring, String billingInterval, String notes, Instant createdAt, Instant updatedAt, long version) {}
}
