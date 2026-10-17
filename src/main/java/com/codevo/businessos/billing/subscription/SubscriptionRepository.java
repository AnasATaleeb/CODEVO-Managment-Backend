package com.codevo.businessos.billing.subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface SubscriptionRepository extends JpaRepository<Subscription,UUID>, JpaSpecificationExecutor<Subscription> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Subscription e where e.id=:id")
    java.util.Optional<Subscription> lockById(UUID id);
    boolean existsByProjectServiceIdAndStatusNotAndIdNot(UUID projectServiceId,String status,UUID id);
}
