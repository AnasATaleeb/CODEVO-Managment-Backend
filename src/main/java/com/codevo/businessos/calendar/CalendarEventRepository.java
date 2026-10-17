package com.codevo.businessos.calendar;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
public interface CalendarEventRepository extends JpaRepository<CalendarEvent,UUID>, JpaSpecificationExecutor<CalendarEvent> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from CalendarEvent e where e.id=:id")
    java.util.Optional<CalendarEvent> lockById(UUID id);
}
