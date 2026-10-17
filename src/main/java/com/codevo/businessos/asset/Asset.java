package com.codevo.businessos.asset;

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
@Table(name="assets")
public class Asset extends AuditedEntity {
    private UUID clientId;
    private UUID projectId;
    @Column(length=255)
    private String name;
    @Column(length=255)
    private String type;
    @Column(length=2048)
    private String url;
    @Column(length=255)
    private String provider;
    @Column(length=255)
    private String identifier;
    private LocalDate renewalDate;
    private LocalDate expiryDate;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
    @Column(length=2048)
    private String credentialReference;
}
