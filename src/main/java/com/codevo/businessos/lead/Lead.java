package com.codevo.businessos.lead;

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
@Table(name="leads")
public class Lead extends AuditedEntity {
    @Column(length=255)
    private String name;
    @Column(length=255)
    private String companyName;
    @Column(length=255)
    private String phone;
    @Column(length=255)
    private String whatsapp;
    @Column(length=255)
    private String email;
    @Column(length=255)
    private String source;
    @Column(length=255)
    private String serviceInterest;
    @Column(precision=19, scale=3)
    private BigDecimal estimatedValue;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Column(length=255)
    private String stage;
    @Column(length=255)
    private String priority;
    private Instant nextFollowUpAt;
    private UUID assignedUserId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String lostReason;
    private UUID convertedClientId;
}
