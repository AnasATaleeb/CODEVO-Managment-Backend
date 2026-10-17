package com.codevo.businessos.billing.payment;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface CheckPaymentRepository extends JpaRepository<CheckPayment,UUID>,JpaSpecificationExecutor<CheckPayment> {
    List<CheckPayment> findByPaymentPartIdIn(Collection<UUID> ids);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from CheckPayment e where e.id=:id") Optional<CheckPayment> lockById(UUID id);
}
