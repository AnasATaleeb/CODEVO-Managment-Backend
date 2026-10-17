package com.codevo.businessos.billing.expense;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface ExpenseMapper {
    ExpenseDtos.View view(Expense entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="projectId", source="projectId")
    @Mapping(target="category", source="category")
    @Mapping(target="description", source="description")
    @Mapping(target="vendor", source="vendor")
    @Mapping(target="amount", source="amount")
    @Mapping(target="currency", source="currency")
    @Mapping(target="expenseDate", source="expenseDate")
    @Mapping(target="recurring", source="recurring")
    @Mapping(target="billingInterval", source="billingInterval")
    @Mapping(target="notes", source="notes")
    void apply(ExpenseDtos.Request request, @MappingTarget Expense entity);
}
