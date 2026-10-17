package com.codevo.businessos.billing.payment;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface PaymentPartRepository extends JpaRepository<PaymentPart,UUID>,JpaSpecificationExecutor<PaymentPart> {
    List<PaymentPart> findByPaymentIdIn(Collection<UUID> ids);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from PaymentPart e where e.id=:id") Optional<PaymentPart> lockById(UUID id);
}
