package com.codevo.businessos.billing.expense;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name="expenses")
public class Expense extends AuditedEntity {
    private UUID clientId;
    private UUID projectId;
    @Column(length=255)
    private String category;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String description;
    @Column(length=255)
    private String vendor;
    @Column(precision=19, scale=3)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private LocalDate expenseDate;
    private Boolean recurring;
    @Column(length=255)
    private String billingInterval;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
}
