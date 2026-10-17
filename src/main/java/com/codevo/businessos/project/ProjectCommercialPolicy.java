package com.codevo.businessos.project;

import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.DomainException;
import com.codevo.businessos.common.Money;
import java.math.BigDecimal;
import java.util.Set;

/** Currency rounding and service billing policy shared by commercial commands and responses. */
public final class ProjectCommercialPolicy {
    private static final Set<String> INTERVALS=Set.of("MONTHLY","QUARTERLY","SEMI_ANNUAL","YEARLY","CUSTOM");
    private ProjectCommercialPolicy() {}

    public record BillingAmounts(BigDecimal invoicedAmount,BigDecimal draftReservedAmount) {
        public static BillingAmounts none() { return new BillingAmounts(BigDecimal.ZERO,BigDecimal.ZERO); }
    }

    public static BigDecimal total(BigDecimal quantity,BigDecimal unitAmount,Currency currency) {
        if(quantity==null || quantity.signum()<0 || quantity.scale()>3 || quantity.precision()-quantity.scale()>9)
            throw DomainException.invalid("Quantity must be positive with up to three decimal places");
        return Money.amount(Money.rounded(quantity.multiply(Money.amount(unitAmount,currency)),currency),currency);
    }

    public static BigDecimal remaining(BigDecimal agreed,BillingAmounts billed,boolean reserveDrafts) {
        BigDecimal amount=agreed.subtract(billed.invoicedAmount());
        if(reserveDrafts) amount=amount.subtract(billed.draftReservedAmount());
        return amount.max(BigDecimal.ZERO);
    }

    public static void requireAvailable(BigDecimal agreed,BillingAmounts billed,BigDecimal requested) {
        if(requested.signum()<=0) throw DomainException.invalid("Selected invoice amount must be positive");
        if(requested.compareTo(remaining(agreed,billed,true))>0)
            throw DomainException.conflict("Selected amount exceeds the service balance available to invoice; refresh to include issued invoices and reserved drafts");
    }

    public static String billingStatus(BigDecimal agreed,BillingAmounts billed) {
        if(billed.invoicedAmount().signum()==0) return "NOT_INVOICED";
        return billed.invoicedAmount().compareTo(agreed)>=0?"FULLY_INVOICED":"PARTIALLY_INVOICED";
    }

    public static String billingInterval(String billingType,String supplied) {
        if(supplied!=null) {
            if(!INTERVALS.contains(supplied)) throw DomainException.invalid("Invalid billing interval");
            return supplied;
        }
        // Legacy CUSTOM also describes non-recurring negotiated work; it is not an interval without explicit months.
        return INTERVALS.contains(billingType) && !"CUSTOM".equals(billingType)?billingType:null;
    }

    public static boolean subscriptionEligible(ProjectService service) {
        String interval=billingInterval(service.getBillingType(),service.getBillingInterval());
        return Boolean.TRUE.equals(service.getRecurring()) && !"CANCELLED".equals(service.getStatus()) && interval!=null
            && (!"CUSTOM".equals(interval) || service.getCustomIntervalMonths()!=null);
    }
}
