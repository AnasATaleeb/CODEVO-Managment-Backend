package com.codevo.businessos.receivables;

import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReceivableDtos {
    private ReceivableDtos() {}
    public record Row(UUID invoiceId, String invoiceNumber, UUID clientId, String clientName,
        UUID projectId, String projectName, String services, Currency currency,
        BigDecimal invoiceTotal, BigDecimal collectedAmount, BigDecimal pendingCheckCoverage,
        BigDecimal outstandingAmount, BigDecimal uncoveredOutstanding, LocalDate dueDate,
        long daysOverdue, String status, String coverageStatus, String nextAction, boolean hasBouncedCheck) {}
    public record Totals(Currency currency, BigDecimal invoiced, BigDecimal collected,
        BigDecimal pendingCheckCoverage, BigDecimal outstandingAmount, BigDecimal uncoveredOutstanding,
        BigDecimal overdue, BigDecimal dueToday, BigDecimal dueNext7Days) {}
    public record Page(List<Row> content, int page, int size, long totalElements, int totalPages,
        List<Totals> totals, LocalDate asOf) {}
}
