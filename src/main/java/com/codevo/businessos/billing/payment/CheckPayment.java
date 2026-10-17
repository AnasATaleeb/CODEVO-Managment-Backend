package com.codevo.businessos.billing.payment;
import com.codevo.businessos.common.AuditedEntity;
import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="check_payments") @Getter @Setter
public class CheckPayment extends AuditedEntity {
    private UUID paymentPartId;
    private String checkNumber;
    private String bankName;
    private String issuerName;
    @Column(precision=19,scale=3)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private LocalDate receivedDate;
    private LocalDate dueDate;
    private LocalDate depositedDate;
    private LocalDate clearedDate;
    private LocalDate bouncedDate;
    private String status;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
    private UUID attachmentFileId;
    private Instant reversedAt;
    @Column(length=1000)
    private String reversalReason;
}
