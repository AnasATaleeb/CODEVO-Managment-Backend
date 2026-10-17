package com.codevo.businessos.project;

import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.*;

public final class ProjectServiceDtos {
    private ProjectServiceDtos() {}
    public record Request(@NotNull UUID projectId,
        @NotNull UUID serviceCatalogItemId,
        @NotBlank @Size(max=255) String customName,
        @Size(max=10000) String description,
        @NotNull @Pattern(regexp="ONE_TIME|MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|INSTALLMENT|SETUP_PLUS_RECURRING|CUSTOM") String billingType,
        @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal price,
        @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal cost,
        @NotNull Currency currency,
        @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantity,
        LocalDate startDate,LocalDate deliveryDate,LocalDate deadline,
        @NotNull @Pattern(regexp="PLANNED|IN_PROGRESS|DELIVERED|COMPLETED|CANCELLED") String status,
        @NotNull Boolean recurring,
        @Size(max=10000) String notes, Long version,
        @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal unitPrice,
        @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal estimatedUnitCost,
        @Pattern(regexp="MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|CUSTOM") String billingInterval,
        @Min(1) @Max(120) Integer customIntervalMonths) {
        public Request(UUID projectId,UUID serviceCatalogItemId,String customName,String description,String billingType,
            BigDecimal price,BigDecimal cost,Currency currency,BigDecimal quantity,LocalDate startDate,LocalDate deliveryDate,
            LocalDate deadline,String status,Boolean recurring,String notes,Long version) {
            this(projectId,serviceCatalogItemId,customName,description,billingType,price,cost,currency,quantity,startDate,deliveryDate,deadline,status,recurring,notes,version,null,null,null,null);
        }
    }
    public record InlineRequest(@NotNull UUID serviceCatalogItemId,@NotBlank @Size(max=255) String customName,
        @Size(max=10000) String description,
        @NotNull @Pattern(regexp="ONE_TIME|MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|INSTALLMENT|SETUP_PLUS_RECURRING|CUSTOM") String billingType,
        @NotNull @DecimalMin("0") @Digits(integer=15,fraction=3) BigDecimal unitPrice,
        @NotNull @DecimalMin("0") @Digits(integer=15,fraction=3) BigDecimal estimatedUnitCost,
        @NotNull Currency currency,@NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantity,
        LocalDate startDate,LocalDate deliveryDate,LocalDate deadline,
        @NotNull @Pattern(regexp="PLANNED|IN_PROGRESS|DELIVERED|COMPLETED|CANCELLED") String status,
        @NotNull Boolean recurring,
        @Pattern(regexp="MONTHLY|QUARTERLY|SEMI_ANNUAL|YEARLY|CUSTOM") String billingInterval,
        @Min(1) @Max(120) Integer customIntervalMonths,@Size(max=10000) String notes) {
        public Request forProject(UUID projectId) {
            return new Request(projectId,serviceCatalogItemId,customName,description,billingType,null,null,currency,quantity,startDate,deliveryDate,deadline,status,recurring,notes,null,unitPrice,estimatedUnitCost,billingInterval,customIntervalMonths);
        }
    }
    public record View(UUID id,UUID projectId,UUID serviceCatalogItemId,String customName,String description,String billingType,
        BigDecimal price,BigDecimal cost,Currency currency,BigDecimal quantity,LocalDate startDate,LocalDate deliveryDate,
        LocalDate deadline,String status,Boolean recurring,String notes,Instant createdAt,Instant updatedAt,long version,
        BigDecimal unitPrice,BigDecimal estimatedUnitCost,String billingInterval,Integer customIntervalMonths,
        BigDecimal agreedAmount,BigDecimal estimatedTotalCost,BigDecimal invoicedAmount,BigDecimal draftReservedAmount,
        BigDecimal uninvoicedAmount,BigDecimal availableToInvoice,String billingStatus,boolean subscriptionEligible) {}
    public record UninvoicedItem(UUID id,UUID projectId,String projectName,UUID clientId,String clientName,String customName,
        Currency currency,BigDecimal agreedAmount,BigDecimal invoicedAmount,BigDecimal uninvoicedAmount,BigDecimal availableToInvoice,
        BigDecimal draftReservedAmount,String billingStatus,String status) {}
    public record UninvoicedTotal(Currency currency,BigDecimal amount,BigDecimal availableAmount,BigDecimal draftReservedAmount) {}
    public record UninvoicedPage(List<UninvoicedItem> content,int page,int size,long totalElements,int totalPages,List<UninvoicedTotal> totals) {}
}
