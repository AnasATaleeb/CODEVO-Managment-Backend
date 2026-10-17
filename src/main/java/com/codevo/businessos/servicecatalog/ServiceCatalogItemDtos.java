package com.codevo.businessos.servicecatalog;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class ServiceCatalogItemDtos {
    private ServiceCatalogItemDtos() {}
    public record Request(@NotBlank @Size(max=255) String name,
        @NotBlank @Size(max=255) String code,
        @NotBlank @Size(max=255) String category,
        @Size(max=10000) String description,
        @NotNull @Pattern(regexp="ONE_TIME|MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|INSTALLMENT|SETUP_PLUS_RECURRING|CUSTOM") String defaultBillingType,
        @NotNull @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal defaultPrice,
        @NotNull @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal defaultCost,
        @NotNull Currency defaultCurrency,
        @NotNull Boolean active, Long version) {}
    public record View(UUID id, String name, String code, String category, String description, String defaultBillingType, BigDecimal defaultPrice, BigDecimal defaultCost, Currency defaultCurrency, Boolean active, Instant createdAt, Instant updatedAt, long version) {}
}
