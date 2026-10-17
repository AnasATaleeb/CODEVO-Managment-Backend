package com.codevo.businessos.billing.subscription;

import java.time.LocalDate;
import com.codevo.businessos.common.DomainException;

public final class SubscriptionPolicy {
    private SubscriptionPolicy() {}
    public record Period(LocalDate start,LocalDate end,LocalDate renewal) {}
    public static Period renew(LocalDate currentEnd,String interval,Integer customMonths) {
        int months=switch(interval) {
            case "MONTHLY" -> 1; case "QUARTERLY" -> 3; case "SEMI_ANNUAL" -> 6; case "YEARLY" -> 12;
            case "CUSTOM" -> customMonths==null?0:customMonths;
            default -> throw DomainException.invalid("Invalid billing interval");
        };
        if(months<1 || months>120) throw DomainException.invalid("Invalid custom interval");
        LocalDate start=currentEnd.plusDays(1);
        LocalDate end=start.plusMonths(months).minusDays(1);
        return new Period(start,end,end.plusDays(1));
    }
}
