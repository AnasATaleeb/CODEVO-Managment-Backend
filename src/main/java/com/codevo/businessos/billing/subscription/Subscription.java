package com.codevo.businessos.billing.subscription;

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
@Table(name="subscriptions")
public class Subscription extends AuditedEntity {
    private boolean invoiceAutomationEnabled;
    private int invoiceLeadDays=7;
    private int invoiceDueDays=7;
    @Column(length=1000) private String invoiceAutomationError;
    private UUID clientId;
    private UUID projectId;
    private UUID projectServiceId;
    @Column(length=255)
    private String name;
    @Column(length=255)
    private String status;
    @Column(length=255)
    private String billingInterval;
    @Column(precision=19, scale=3)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private LocalDate startDate;
    private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;
    private LocalDate renewalDate;
    private Boolean autoRenew;
    private Integer gracePeriodDays;
    private Integer customIntervalMonths;
    private Instant cancelledAt;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
}
