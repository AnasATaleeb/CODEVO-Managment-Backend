package com.codevo.businessos.calendar;

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
@Table(name="calendar_events")
public class CalendarEvent extends AuditedEntity {
    private UUID clientId;
    private UUID projectId;
    @Column(length=255)
    private String title;
    @Column(length=255)
    private String type;
    private Instant startAt;
    private Instant endAt;
    private UUID assignedUserId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String notes;
}
