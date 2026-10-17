package com.codevo.businessos.billing.subscription;

import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.ListFilter;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RenewalFilter extends ListFilter {
    private Currency currency;
    private java.util.UUID subscriptionId;
    public RenewalFilter() { setSort("renewalDate");setDirection("asc"); }
}
