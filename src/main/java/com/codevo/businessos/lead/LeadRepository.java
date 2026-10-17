package com.codevo.businessos.lead;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface LeadRepository extends JpaRepository<Lead,UUID>, JpaSpecificationExecutor<Lead> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Lead e where e.id=:id")
    java.util.Optional<Lead> lockById(UUID id);
}
