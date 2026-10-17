package com.codevo.businessos.receivables;

import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.ListFilter;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReceivableFilter extends ListFilter {
    private Currency currency;
    private java.util.UUID invoiceId;
    public ReceivableFilter() { setSort("dueDate"); setDirection("asc"); }
}
