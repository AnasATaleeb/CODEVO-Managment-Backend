package com.codevo.businessos.calendar;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class CalendarEventDtos {
    private CalendarEventDtos() {}
    public record Request( UUID clientId,
         UUID projectId,
        @NotBlank @Size(max=255) String title,
        @NotNull @Pattern(regexp="MEETING|MANUAL|DOMAIN_EXPIRY|HOSTING_EXPIRY") String type,
         Instant startAt,
         Instant endAt,
         UUID assignedUserId,
        @Size(max=10000) String notes, Long version) {}
    public record View(UUID id, UUID clientId, UUID projectId, String title, String type, Instant startAt, Instant endAt, UUID assignedUserId, String notes, Instant createdAt, Instant updatedAt, long version) {}
}
