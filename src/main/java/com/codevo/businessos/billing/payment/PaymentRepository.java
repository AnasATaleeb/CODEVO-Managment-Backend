package com.codevo.businessos.billing.payment;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface PaymentRepository extends JpaRepository<Payment,UUID>,JpaSpecificationExecutor<Payment> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from Payment e where e.id=:id") Optional<Payment> lockById(UUID id);
}
