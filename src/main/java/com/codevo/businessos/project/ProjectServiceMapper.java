package com.codevo.businessos.project;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface ProjectServiceMapper {
    default ProjectServiceDtos.View view(ProjectService e,ProjectCommercialPolicy.BillingAmounts billed) {
        var agreed=ProjectCommercialPolicy.total(e.getQuantity(),e.getPrice(),e.getCurrency());
        boolean financial=com.codevo.businessos.security.Permissions.currentHas("FINANCE_READ");
        return new ProjectServiceDtos.View(e.getId(),e.getProjectId(),e.getServiceCatalogItemId(),e.getCustomName(),e.getDescription(),e.getBillingType(),
            financial?e.getPrice():null,financial?e.getCost():null,e.getCurrency(),e.getQuantity(),e.getStartDate(),e.getDeliveryDate(),e.getDeadline(),e.getStatus(),e.getRecurring(),e.getNotes(),e.getCreatedAt(),e.getUpdatedAt(),e.getVersion(),
            financial?e.getPrice():null,financial?e.getCost():null,ProjectCommercialPolicy.billingInterval(e.getBillingType(),e.getBillingInterval()),e.getCustomIntervalMonths(),financial?agreed:null,
            financial?ProjectCommercialPolicy.total(e.getQuantity(),e.getCost(),e.getCurrency()):null,financial?billed.invoicedAmount():null,financial?billed.draftReservedAmount():null,
            financial?ProjectCommercialPolicy.remaining(agreed,billed,false):null,financial?ProjectCommercialPolicy.remaining(agreed,billed,true):null,financial?ProjectCommercialPolicy.billingStatus(agreed,billed):null,financial && ProjectCommercialPolicy.subscriptionEligible(e));
    }
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="projectId", source="projectId")
    @Mapping(target="serviceCatalogItemId", source="serviceCatalogItemId")
    @Mapping(target="customName", source="customName")
    @Mapping(target="description", source="description")
    @Mapping(target="billingType", source="billingType")
    @Mapping(target="billingInterval", source="billingInterval")
    @Mapping(target="customIntervalMonths", source="customIntervalMonths")
    @Mapping(target="price", source="price")
    @Mapping(target="cost", source="cost")
    @Mapping(target="currency", source="currency")
    @Mapping(target="quantity", source="quantity")
    @Mapping(target="startDate", source="startDate")
    @Mapping(target="deliveryDate", source="deliveryDate")
    @Mapping(target="deadline", source="deadline")
    @Mapping(target="status", source="status")
    @Mapping(target="recurring", source="recurring")
    @Mapping(target="notes", source="notes")
    void apply(ProjectServiceDtos.Request request, @MappingTarget ProjectService entity);
}
