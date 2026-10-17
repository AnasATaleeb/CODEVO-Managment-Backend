package com.codevo.businessos.project;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ProjectRepository extends JpaRepository<Project,UUID>, JpaSpecificationExecutor<Project> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Project e where e.id=:id")
    java.util.Optional<Project> lockById(UUID id);
}
