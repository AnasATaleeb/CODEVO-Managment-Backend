package com.codevo.businessos.billing.subscription;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class SubscriptionDtos {
    private SubscriptionDtos() {}
    public record Request(@NotNull UUID clientId,
         UUID projectId,
         UUID projectServiceId,
        @NotBlank @Size(max=255) String name,
        @NotNull @Pattern(regexp="DRAFT|ACTIVE|PAST_DUE|EXPIRED|PAUSED|CANCELLED") String status,
        @NotNull @Pattern(regexp="MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|CUSTOM") String billingInterval,
        @NotNull @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal amount,
        @NotNull Currency currency,
        @NotNull LocalDate startDate,
        @NotNull LocalDate currentPeriodStart,
        @NotNull LocalDate currentPeriodEnd,
        @NotNull LocalDate renewalDate,
        @NotNull Boolean autoRenew,
        @NotNull @Min(0) @Max(90) Integer gracePeriodDays,
        @Min(1) @Max(120) Integer customIntervalMonths,
         Instant cancelledAt,
        @Size(max=10000) String notes, Long version) {}
    public record Renewal(@NotNull Long version,@NotBlank @Size(min=3,max=1000) String reason) {}
    public record FromService(@NotNull UUID projectServiceId, LocalDate startDate,
        @Pattern(regexp="DRAFT|ACTIVE") String status, Boolean autoRenew, @Min(0) @Max(90) Integer gracePeriodDays) {}
    public record RenewalInvoice(@NotNull Long version,@NotNull LocalDate issueDate,@NotNull LocalDate dueDate,
        @Pattern(regexp="DRAFT|ISSUED") String status) {}
    public record View(UUID id, UUID clientId, UUID projectId, UUID projectServiceId, String name, String status, String billingInterval, BigDecimal amount, Currency currency, LocalDate startDate, LocalDate currentPeriodStart, LocalDate currentPeriodEnd, LocalDate renewalDate, Boolean autoRenew, Integer gracePeriodDays, Integer customIntervalMonths, Instant cancelledAt, String notes, Instant createdAt, Instant updatedAt, long version, boolean invoiceAutomationEnabled, int invoiceLeadDays, int invoiceDueDays, String invoiceAutomationError) {}
}
