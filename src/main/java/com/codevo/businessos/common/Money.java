package com.codevo.businessos.common;

import java.math.*;

public final class Money {
    private Money() {}
    public static BigDecimal amount(BigDecimal value, Currency currency) {
        if (value == null || currency == null || value.signum() < 0 || value.precision() > 18) throw DomainException.invalid("Invalid monetary amount");
        try { return value.setScale(currency == Currency.JOD ? 3 : 2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw DomainException.invalid("Too many decimal places for "+currency); }
    }
    public static BigDecimal rounded(BigDecimal value, Currency currency) {
        return value.setScale(currency == Currency.JOD ? 3 : 2,RoundingMode.HALF_UP);
    }
    public static BigDecimal monthly(BigDecimal amount, String interval, Integer customMonths) {
        int months = switch(interval) {
            case "MONTHLY" -> 1; case "QUARTERLY" -> 3; case "SEMI_ANNUAL" -> 6; case "YEARLY" -> 12;
            case "CUSTOM" -> customMonths == null ? 0 : customMonths;
            default -> throw DomainException.invalid("Invalid billing interval");
        };
        if (months <= 0 || months > 120) throw DomainException.invalid("Billing months must be between 1 and 120");
        return amount.divide(BigDecimal.valueOf(months),8,RoundingMode.HALF_UP);
    }
}
