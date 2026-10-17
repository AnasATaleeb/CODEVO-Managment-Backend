package com.codevo.businessos.project;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;
import com.codevo.businessos.project.ProjectServiceDtos.InlineRequest;

public final class ProjectDtos {
    private ProjectDtos() {}
    public record Request(@NotNull UUID clientId,
        @NotBlank @Size(max=255) String name,
        @NotBlank @Size(max=255) String projectCode,
        @Size(max=10000) String description,
        @NotNull @Pattern(regexp="DRAFT|PLANNED|WAITING_CLIENT|IN_PROGRESS|INTERNAL_REVIEW|CLIENT_REVIEW|REVISION|DELIVERED|COMPLETED|ON_HOLD|CANCELLED") String status,
        @NotNull @Pattern(regexp="LOW|NORMAL|HIGH|URGENT") String priority,
         LocalDate startDate,
         LocalDate deadline,
         Instant completedAt,
         UUID assignedUserId,
        @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal estimatedPrice,
        @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal estimatedCost,
        @NotNull Currency currency,
        @Size(max=10000) String notes, Long version,@Size(max=50) List<@Valid InlineRequest> services) {
        public Request(UUID clientId,String name,String projectCode,String description,String status,String priority,LocalDate startDate,
            LocalDate deadline,Instant completedAt,UUID assignedUserId,BigDecimal estimatedPrice,BigDecimal estimatedCost,Currency currency,String notes,Long version) {
            this(clientId,name,projectCode,description,status,priority,startDate,deadline,completedAt,assignedUserId,estimatedPrice,estimatedCost,currency,notes,version,null);
        }
    }
    public record CommercialTotal(Currency currency,BigDecimal contractValue,BigDecimal estimatedCost,long serviceCount) {}
    public record View(UUID id, UUID clientId, String name, String projectCode, String description, String status, String priority, LocalDate startDate, LocalDate deadline, Instant completedAt, UUID assignedUserId, BigDecimal estimatedPrice, BigDecimal estimatedCost, Currency currency, String notes, Instant createdAt, Instant updatedAt, long version,BigDecimal calculatedContractValue,BigDecimal calculatedEstimatedCost,String financialSource,List<CommercialTotal> commercialTotals) {}
}
