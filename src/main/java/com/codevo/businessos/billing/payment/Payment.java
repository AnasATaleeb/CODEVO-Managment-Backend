package com.codevo.businessos.billing.payment;
import com.codevo.businessos.common.AuditedEntity;
import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="payments") @Getter @Setter
public class Payment extends AuditedEntity {
    private UUID clientId;
    private UUID invoiceId;
    private LocalDate paymentDate;
    private String status;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
    private Instant reversedAt;
    @Column(length=1000)
    private String reversalReason;
}
