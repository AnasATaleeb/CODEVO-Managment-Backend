package com.codevo.businessos.servicecatalog;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ServiceCatalogItemRepository extends JpaRepository<ServiceCatalogItem,UUID>, JpaSpecificationExecutor<ServiceCatalogItem> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from ServiceCatalogItem e where e.id=:id")
    java.util.Optional<ServiceCatalogItem> lockById(UUID id);
}
