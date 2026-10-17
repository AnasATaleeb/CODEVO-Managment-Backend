package com.codevo.businessos.billing.payment;
import com.codevo.businessos.common.AuditedEntity;
import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="payment_parts") @Getter @Setter
public class PaymentPart extends AuditedEntity {
    private UUID paymentId;
    private String method;
    @Column(precision=19,scale=3)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private String bankName;
    private String referenceNumber;
    private LocalDate transactionDate;
    @Column(precision=19,scale=8)
    private BigDecimal allocationExchangeRate;
    @Column(precision=19,scale=3)
    private BigDecimal allocationAmount;
    @Column(precision=19,scale=8)
    private BigDecimal exchangeRateToReportingCurrency;
    @Enumerated(EnumType.STRING)
    private Currency reportingCurrency;
    @Column(precision=19,scale=3)
    private BigDecimal reportingAmount;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
    private UUID attachmentFileId;
}
