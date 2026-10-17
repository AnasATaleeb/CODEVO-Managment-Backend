package com.codevo.businessos.client;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ClientNoteRepository extends JpaRepository<ClientNote,UUID>, JpaSpecificationExecutor<ClientNote> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from ClientNote e where e.id=:id")
    java.util.Optional<ClientNote> lockById(UUID id);
}
