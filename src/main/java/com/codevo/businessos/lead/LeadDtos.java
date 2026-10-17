package com.codevo.businessos.lead;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class LeadDtos {
    private LeadDtos() {}
    public record Request(@NotBlank @Size(max=255) String name,
        @Size(max=255) String companyName,
        @Size(max=255) String phone,
        @Size(max=255) String whatsapp,
        @Email @Size(max=255) String email,
        @Size(max=255) String source,
        @Size(max=255) String serviceInterest,
        @NotNull @DecimalMin("0.0") @Digits(integer=15,fraction=3) BigDecimal estimatedValue,
        @NotNull Currency currency,
        @NotNull @Pattern(regexp="NEW|CONTACTED|QUALIFIED|PROPOSAL_SENT|NEGOTIATION|WON|LOST") String stage,
        @NotNull @Pattern(regexp="LOW|NORMAL|HIGH|URGENT") String priority,
         Instant nextFollowUpAt,
         UUID assignedUserId,
        @Size(max=10000) String notes,
        @Size(max=10000) String lostReason, Long version) {}
    public record View(UUID id, String name, String companyName, String phone, String whatsapp, String email, String source, String serviceInterest, BigDecimal estimatedValue, Currency currency, String stage, String priority, Instant nextFollowUpAt, UUID assignedUserId, String notes, String lostReason, UUID convertedClientId, Instant createdAt, Instant updatedAt, long version) {}
}
