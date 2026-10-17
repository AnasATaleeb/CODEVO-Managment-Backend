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
@Table(name="client_notes")
public class ClientNote extends AuditedEntity {
    private UUID clientId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String body;
}
