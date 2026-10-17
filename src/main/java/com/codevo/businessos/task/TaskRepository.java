package com.codevo.businessos.task;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface TaskRepository extends JpaRepository<Task,UUID>, JpaSpecificationExecutor<Task> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Task e where e.id=:id")
    java.util.Optional<Task> lockById(UUID id);
}
