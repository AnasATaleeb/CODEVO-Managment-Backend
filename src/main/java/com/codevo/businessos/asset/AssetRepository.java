package com.codevo.businessos.asset;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface AssetRepository extends JpaRepository<Asset,UUID>, JpaSpecificationExecutor<Asset> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Asset e where e.id=:id")
    java.util.Optional<Asset> lockById(UUID id);
}
