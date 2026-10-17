package com.codevo.businessos.common;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditedEntity {
    @Id protected UUID id = UUID.randomUUID();
    @CreatedDate @Column(nullable = false, updatable = false) protected Instant createdAt;
    @LastModifiedDate @Column(nullable = false) protected Instant updatedAt;
    @CreatedBy @Column(updatable = false) protected UUID createdBy;
    @LastModifiedBy protected UUID updatedBy;
    @Version protected long version;
}
