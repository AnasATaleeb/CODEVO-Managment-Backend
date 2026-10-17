package com.codevo.businessos.billing.subscription;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface SubscriptionMapper {
    SubscriptionDtos.View view(Subscription entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="projectId", source="projectId")
    @Mapping(target="projectServiceId", source="projectServiceId")
    @Mapping(target="name", source="name")
    @Mapping(target="status", source="status")
    @Mapping(target="billingInterval", source="billingInterval")
    @Mapping(target="amount", source="amount")
    @Mapping(target="currency", source="currency")
    @Mapping(target="startDate", source="startDate")
    @Mapping(target="currentPeriodStart", source="currentPeriodStart")
    @Mapping(target="currentPeriodEnd", source="currentPeriodEnd")
    @Mapping(target="renewalDate", source="renewalDate")
    @Mapping(target="autoRenew", source="autoRenew")
    @Mapping(target="gracePeriodDays", source="gracePeriodDays")
    @Mapping(target="customIntervalMonths", source="customIntervalMonths")
    @Mapping(target="cancelledAt", source="cancelledAt")
    @Mapping(target="notes", source="notes")
    void apply(SubscriptionDtos.Request request, @MappingTarget Subscription entity);
}
