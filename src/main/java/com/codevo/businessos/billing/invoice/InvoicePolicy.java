package com.codevo.businessos.billing.invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.codevo.businessos.common.DomainException;
public final class InvoicePolicy {
    private InvoicePolicy() {}
    public static String status(BigDecimal total,BigDecimal collected,LocalDate due,LocalDate today) {
        if(collected.compareTo(total)>0) throw DomainException.conflict("Payment exceeds invoice total");
        if(collected.compareTo(total)==0) return "PAID";
        if(due.isBefore(today)) return "OVERDUE";
        return collected.signum()>0?"PARTIALLY_PAID":"ISSUED";
    }
    public static void coverage(BigDecimal total,BigDecimal collected,BigDecimal pending) {
        if(collected.add(pending).compareTo(total)>0) throw DomainException.conflict("Collected and pending check coverage exceeds invoice total");
    }
}
