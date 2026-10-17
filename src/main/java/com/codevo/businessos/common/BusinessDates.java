package com.codevo.businessos.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Instants are stored in UTC; business dates use the configured organization timezone. */
@Component
public class BusinessDates {
    private final Clock clock;
    private final JdbcTemplate jdbc;
    public BusinessDates(Clock clock, JdbcTemplate jdbc) { this.clock=clock; this.jdbc=jdbc; }
    public ZoneId zone() {
        String zone=jdbc.queryForObject("select timezone from organization_settings where id=1",String.class);
        return ZoneId.of(zone);
    }
    public LocalDate today() {
        return clock.instant().atZone(zone()).toLocalDate();
    }
}
