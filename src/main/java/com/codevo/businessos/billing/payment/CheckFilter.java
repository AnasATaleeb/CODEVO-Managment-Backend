package com.codevo.businessos.billing.payment;

import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.ListFilter;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckFilter extends ListFilter {
    private Currency currency;
    private String bankName;
}
