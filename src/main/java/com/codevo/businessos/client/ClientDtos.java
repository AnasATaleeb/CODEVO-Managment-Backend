package com.codevo.businessos.client;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class ClientDtos {
    private ClientDtos() {}
    public record Request(@NotBlank @Size(max=255) String displayName,
        @Size(max=255) String companyName,
        @Size(max=255) String contactPerson,
        @Email @Size(max=255) String email,
        @Size(max=255) String phone,
        @Size(max=255) String whatsapp,
        @Size(max=255) String country,
        @Size(max=255) String city,
        @Size(max=2048) @Pattern(regexp="^(https?://.*)?$") String website,
        @Size(max=255) String source,
        @NotNull @Pattern(regexp="LEAD|PROSPECT|ACTIVE|ON_HOLD|INACTIVE|COMPLETED|LOST") String status,
        @Size(max=10000) String notes,
        @NotNull Currency preferredCurrency,
        @Size(max=1000) String tags,
         UUID assignedUserId,
         LocalDate firstContactDate,
         LocalDate clientSinceDate, Long version) {}
    public record View(UUID id, String displayName, String companyName, String contactPerson, String email, String phone, String whatsapp, String country, String city, String website, String source, String status, String notes, Currency preferredCurrency, String tags, UUID assignedUserId, LocalDate firstContactDate, LocalDate clientSinceDate, Instant createdAt, Instant updatedAt, long version, UUID mergedIntoId, Instant mergedAt) {}
}
