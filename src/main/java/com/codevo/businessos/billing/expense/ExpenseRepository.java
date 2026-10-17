package com.codevo.businessos.billing.expense;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface ExpenseRepository extends JpaRepository<Expense,UUID>, JpaSpecificationExecutor<Expense> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Expense e where e.id=:id")
    java.util.Optional<Expense> lockById(UUID id);
}
