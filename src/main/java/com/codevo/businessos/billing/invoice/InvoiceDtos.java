package com.codevo.businessos.billing.invoice;
import com.codevo.businessos.common.Currency;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public final class InvoiceDtos {
    private InvoiceDtos() {}
    public record ItemRequest(UUID projectServiceId,@NotBlank @Size(max=1000) String description,
        @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantity,
        @NotNull @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal unitPrice) {}
    public record Request(@NotNull UUID clientId,UUID projectId,
        @NotNull @Pattern(regexp="DRAFT|ISSUED") String status,@NotNull LocalDate issueDate,@NotNull LocalDate dueDate,
        @NotNull Currency currency,@NotNull @DecimalMin("0") BigDecimal discount,@NotNull @DecimalMin("0") BigDecimal tax,
        @Size(max=10000) String notes,@NotEmpty @Size(max=100) List<@Valid ItemRequest> items) {}
    public record ItemView(UUID id,UUID projectServiceId,String description,BigDecimal quantity,BigDecimal unitPrice,BigDecimal lineTotal) {}
    public record View(UUID id,UUID clientId,UUID projectId,String invoiceNumber,String status,LocalDate issueDate,LocalDate dueDate,
        Currency currency,BigDecimal subtotal,BigDecimal discount,BigDecimal tax,BigDecimal total,BigDecimal paidAmount,
        BigDecimal balanceDue,BigDecimal pendingCheckAmount,String notes,List<ItemView> items,Instant createdAt,long version,UUID originalClientId,String billingClientName,String billingClientEmail) {}
    public record Reason(@NotBlank @Size(min=3,max=1000) String reason) {}
    public record ServiceSelection(@NotNull UUID projectServiceId,
        @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=3) BigDecimal amount,
        @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantity) {}
    public record FromProjectRequest(@NotNull @Pattern(regexp="DRAFT|ISSUED") String status,
        @NotNull LocalDate issueDate,@NotNull LocalDate dueDate,@NotNull Currency currency,
        @NotNull @DecimalMin("0") @Digits(integer=15,fraction=3) BigDecimal discount,
        @NotNull @DecimalMin("0") @Digits(integer=15,fraction=3) BigDecimal tax,
        @Size(max=10000) String notes,@NotEmpty @Size(max=100) List<@Valid ServiceSelection> services) {}
}
