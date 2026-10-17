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
@Table(name="project_services")
public class ProjectService extends AuditedEntity {
    private UUID projectId;
    private UUID serviceCatalogItemId;
    @Column(length=255)
    private String customName;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String description;
    @Column(length=255)
    private String billingType;
    @Column(length=30)
    private String billingInterval;
    private Integer customIntervalMonths;
    @Column(precision=19, scale=3)
    private BigDecimal price;
    @Column(precision=19, scale=3)
    private BigDecimal cost;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Column(precision=19, scale=3)
    private BigDecimal quantity;
    private LocalDate startDate;
    private LocalDate deliveryDate;
    private LocalDate deadline;
    @Column(length=255)
    private String status;
    private Boolean recurring;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
}
