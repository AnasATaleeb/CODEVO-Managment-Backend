package com.codevo.businessos.analytics;
import java.time.*;
import java.util.UUID;
import lombok.*;
import com.codevo.businessos.common.DomainException;
@Getter @Setter
public class AnalyticsFilter {
    private LocalDate from;
    private LocalDate to;
    private UUID clientId;
    private UUID projectId;
    public void validate() {
        if(from!=null && to!=null && (from.isAfter(to) || from.plusYears(10).isBefore(to))) throw DomainException.invalid("Invalid date range; maximum 10 years");
    }
}
