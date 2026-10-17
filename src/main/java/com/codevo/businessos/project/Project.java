package com.codevo.businessos.project;

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
@Table(name="projects")
public class Project extends AuditedEntity {
    private UUID clientId;
    @Column(length=255)
    private String name;
    @Column(length=255)
    private String projectCode;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String description;
    @Column(length=255)
    private String status;
    @Column(length=255)
    private String priority;
    private LocalDate startDate;
    private LocalDate deadline;
    private Instant completedAt;
    private UUID assignedUserId;
    @Column(precision=19, scale=3)
    private BigDecimal estimatedPrice;
    @Column(precision=19, scale=3)
    private BigDecimal estimatedCost;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
}
