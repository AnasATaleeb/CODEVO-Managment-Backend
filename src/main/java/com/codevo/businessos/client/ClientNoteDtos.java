package com.codevo.businessos.client;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class ClientNoteDtos {
    private ClientNoteDtos() {}
    public record Request(@NotNull UUID clientId,
        @Size(max=10000) String body, Long version) {}
    public record View(UUID id, UUID clientId, String body, Instant createdAt, Instant updatedAt, long version) {}
}
