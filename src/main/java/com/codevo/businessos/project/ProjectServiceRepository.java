package com.codevo.businessos.project;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ProjectServiceRepository extends JpaRepository<ProjectService,UUID>, JpaSpecificationExecutor<ProjectService> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from ProjectService e where e.id=:id")
    java.util.Optional<ProjectService> lockById(UUID id);
}
