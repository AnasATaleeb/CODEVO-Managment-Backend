package com.codevo.businessos.billing.payment;
import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public final class PaymentDtos {
    private PaymentDtos() {}
    public record CheckRequest(@NotBlank @Size(max=255) String checkNumber,@NotBlank @Size(max=255) String bankName,
        @Size(max=255) String issuerName,@NotNull LocalDate receivedDate,@NotNull LocalDate dueDate,@Size(max=10000) String notes,UUID attachmentFileId) {}
    public record PartRequest(@NotNull @Pattern(regexp="CASH|BANK_TRANSFER|CHECK|CARD|PAYPAL|STRIPE|OTHER") String method,
        @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=3) BigDecimal amount,@NotNull Currency currency,
        @Size(max=255) String bankName,@Size(max=255) String referenceNumber,LocalDate transactionDate,
        @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=8) BigDecimal allocationExchangeRate,
        @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=8) BigDecimal exchangeRateToReportingCurrency,
        @Size(max=10000) String notes,UUID attachmentFileId,@Valid CheckRequest check) {}
    public record Request(@NotNull UUID clientId,UUID invoiceId,@NotNull LocalDate paymentDate,@Size(max=10000) String notes,
        @NotEmpty @Size(max=100) List<@Valid PartRequest> parts) {}
    public record CheckView(UUID id,UUID paymentPartId,String checkNumber,BigDecimal amount,Currency currency,String bankName,
        String issuerName,LocalDate receivedDate,LocalDate dueDate,LocalDate depositedDate,LocalDate clearedDate,LocalDate bouncedDate,
        String status,String notes,UUID attachmentFileId,Instant reversedAt,String reversalReason,long version) {}
    public record PartView(UUID id,String method,BigDecimal amount,Currency currency,String bankName,String referenceNumber,
        LocalDate transactionDate,BigDecimal allocationExchangeRate,BigDecimal allocationAmount,BigDecimal exchangeRateToReportingCurrency,
        Currency reportingCurrency,BigDecimal reportingAmount,String notes,UUID attachmentFileId,CheckView check) {}
    public record View(UUID id,UUID clientId,UUID invoiceId,LocalDate paymentDate,String status,String notes,
        List<PartView> parts,Instant createdAt,Instant reversedAt,String reversalReason,long version) {}
    public record Transition(@NotNull @Pattern(regexp="DEPOSITED|CLEARED|BOUNCED|CANCELLED") String status,@NotNull LocalDate date,
        @Size(max=1000) String reason,@NotNull Long version) {}
    public record Reason(@NotBlank @Size(min=3,max=1000) String reason) {}
}
