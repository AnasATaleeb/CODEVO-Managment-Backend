package com.codevo.businessos.config;

import java.time.Clock;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
@EnableConfigurationProperties(AppProperties.class)
public class CoreConfig {
    @Bean org.springframework.jdbc.core.JdbcTemplate jdbcTemplate(javax.sql.DataSource source) { return new UuidJdbcTemplate(source); }
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean AuditorAware<UUID> auditor() {
        return () -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return Optional.empty();
            try { return Optional.of(UUID.fromString(auth.getName())); } catch(IllegalArgumentException e) { return Optional.empty(); }
        };
    }
}
