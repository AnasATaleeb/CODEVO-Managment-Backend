package com.codevo.businessos.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix="app")
public record AppProperties(@NotBlank @Size(min=32) String jwtSecret, @NotNull Duration accessTokenTtl,
                            @NotNull Duration refreshTokenTtl, @NotBlank String frontendUrl, boolean secureCookies,
                            @NotBlank String storagePath, boolean seed, String bootstrapOwnerEmail, String bootstrapOwnerPassword) {}
