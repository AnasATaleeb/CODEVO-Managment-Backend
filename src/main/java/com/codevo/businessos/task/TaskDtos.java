package com.codevo.businessos.task;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class TaskDtos {
    private TaskDtos() {}
    public record Request( UUID projectId,
         UUID clientId,
        @NotBlank @Size(max=255) String title,
        @Size(max=10000) String description,
        @NotNull @Pattern(regexp="TODO|IN_PROGRESS|BLOCKED|WAITING_CLIENT|REVIEW|DONE|CANCELLED") String status,
        @NotNull @Pattern(regexp="LOW|NORMAL|HIGH|URGENT") String priority,
         UUID assignedUserId,
         Instant dueAt,
         Instant completedAt,
        @NotNull @Min(0) @Max(100000) Integer sortOrder, Long version) {}
    public record View(UUID id, UUID projectId, UUID clientId, String title, String description, String status, String priority, UUID assignedUserId, Instant dueAt, Instant completedAt, Integer sortOrder, Instant createdAt, Instant updatedAt, long version) {}
}
