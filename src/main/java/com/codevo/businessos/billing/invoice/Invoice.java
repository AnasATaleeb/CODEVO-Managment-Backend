package com.codevo.businessos.billing.invoice;
import com.codevo.businessos.common.AuditedEntity;
import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="invoices") @Getter @Setter
public class Invoice extends AuditedEntity {
    private UUID clientId;
    private UUID originalClientId;
    private String billingClientName;
    private String billingClientEmail;
    private UUID projectId;
    private String invoiceNumber;
    private String status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING) private Currency currency;
    @Column(precision=19,scale=3) private BigDecimal subtotal;
    @Column(precision=19,scale=3) private BigDecimal discount;
    @Column(precision=19,scale=3) private BigDecimal tax;
    @Column(precision=19,scale=3) private BigDecimal total;
    @Column(precision=19,scale=3) private BigDecimal paidAmount=BigDecimal.ZERO;
    @Column(precision=19,scale=3) private BigDecimal pendingCheckAmount=BigDecimal.ZERO;
    @Column(precision=19,scale=3) private BigDecimal balanceDue;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext") private String notes;
    @Column(length=1000) private String voidReason;
}
