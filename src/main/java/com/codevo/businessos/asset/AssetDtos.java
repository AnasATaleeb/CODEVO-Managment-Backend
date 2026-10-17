package com.codevo.businessos.asset;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class AssetDtos {
    private AssetDtos() {}
    public record Request(@NotNull UUID clientId,
         UUID projectId,
        @NotBlank @Size(max=255) String name,
        @NotNull @Pattern(regexp="DOMAIN|HOSTING|SERVER|DATABASE|WEBSITE|BACKEND_API|ANDROID_APP|IOS_APP|GOOGLE_PLAY|APP_STORE|GITHUB_REPOSITORY|FIREBASE_PROJECT|VIMEO|SOCIAL_ACCOUNT|DESIGN_RESOURCE|OTHER") String type,
        @Size(max=2048) @Pattern(regexp="^(https?://.*)?$") String url,
        @Size(max=255) String provider,
        @Size(max=255) String identifier,
         LocalDate renewalDate,
         LocalDate expiryDate,
        @Size(max=10000) String notes,
        @Size(max=2048) String credentialReference, Long version) {}
    public record View(UUID id, UUID clientId, UUID projectId, String name, String type, String url, String provider, String identifier, LocalDate renewalDate, LocalDate expiryDate, String notes, String credentialReference, Instant createdAt, Instant updatedAt, long version) {}
}
