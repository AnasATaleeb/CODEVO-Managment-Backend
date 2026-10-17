package com.codevo.businessos.servicecatalog;

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
@Table(name="service_catalog")
public class ServiceCatalogItem extends AuditedEntity {
    @Column(length=255)
    private String name;
    @Column(length=255)
    private String code;
    @Column(length=255)
    private String category;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String description;
    @Column(length=255)
    private String defaultBillingType;
    @Column(precision=19, scale=3)
    private BigDecimal defaultPrice;
    @Column(precision=19, scale=3)
    private BigDecimal defaultCost;
    @Enumerated(EnumType.STRING)
    private Currency defaultCurrency;
    private Boolean active;
}
