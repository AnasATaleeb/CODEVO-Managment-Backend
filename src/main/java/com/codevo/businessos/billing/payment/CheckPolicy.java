package com.codevo.businessos.billing.payment;
import java.util.Set;
import com.codevo.businessos.common.DomainException;
public final class CheckPolicy {
    private CheckPolicy() {}
    public static boolean collectible(String status) { return "CLEARED".equals(status); }
    public static boolean pending(String status) { return Set.of("PENDING","DEPOSITED").contains(status); }
    public static void transition(String from,String to) {
        if(!pending(from) || !Set.of("DEPOSITED","CLEARED","BOUNCED","CANCELLED").contains(to) || from.equals(to))
            throw DomainException.conflict("Invalid check transition; cleared checks require an explicit reversal");
    }
}
