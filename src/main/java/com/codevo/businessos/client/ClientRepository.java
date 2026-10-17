package com.codevo.businessos.client;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ClientRepository extends JpaRepository<Client,UUID>, JpaSpecificationExecutor<Client> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Client e where e.id=:id")
    java.util.Optional<Client> lockById(UUID id);
}
