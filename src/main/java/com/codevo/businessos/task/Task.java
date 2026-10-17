package com.codevo.businessos.task;

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
@Table(name="tasks")
public class Task extends AuditedEntity {
    private UUID projectId;
    private UUID clientId;
    @Column(length=255)
    private String title;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR) @Column(length=10000,columnDefinition="longtext")
    private String description;
    @Column(length=255)
    private String status;
    @Column(length=255)
    private String priority;
    private UUID assignedUserId;
    private Instant dueAt;
    private Instant completedAt;
    private Integer sortOrder;
}
