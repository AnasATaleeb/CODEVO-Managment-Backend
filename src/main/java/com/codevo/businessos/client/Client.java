package com.codevo.businessos.client;

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
@Table(name="clients")
public class Client extends AuditedEntity {
    private UUID mergedIntoId;
    private Instant mergedAt;
    public void touchForMerge(Instant at) { this.updatedAt=at; }
    @Column(length=255)
    private String displayName;
    @Column(length=255)
    private String companyName;
    @Column(length=255)
    private String contactPerson;
    @Column(length=255)
    private String email;
    @Column(length=255)
    private String phone;
    @Column(length=255)
    private String whatsapp;
    @Column(length=255)
    private String country;
    @Column(length=255)
    private String city;
    @Column(length=2048)
    private String website;
    @Column(length=255)
    private String source;
    @Column(length=255)
    private String status;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
    @Enumerated(EnumType.STRING)
    private Currency preferredCurrency;
    @Column(length=1000)
    private String tags;
    private UUID assignedUserId;
    private LocalDate firstContactDate;
    private LocalDate clientSinceDate;
}
