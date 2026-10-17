package com.codevo.businessos.servicecatalog;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface ServiceCatalogItemMapper {
    default ServiceCatalogItemDtos.View view(ServiceCatalogItem e) {
        boolean financial=com.codevo.businessos.security.Permissions.currentHas("FINANCE_READ");
        return new ServiceCatalogItemDtos.View(e.getId(),e.getName(),e.getCode(),e.getCategory(),e.getDescription(),e.getDefaultBillingType(),financial?e.getDefaultPrice():null,financial?e.getDefaultCost():null,e.getDefaultCurrency(),e.getActive(),e.getCreatedAt(),e.getUpdatedAt(),e.getVersion());
    }
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="name", source="name")
    @Mapping(target="code", source="code")
    @Mapping(target="category", source="category")
    @Mapping(target="description", source="description")
    @Mapping(target="defaultBillingType", source="defaultBillingType")
    @Mapping(target="defaultPrice", source="defaultPrice")
    @Mapping(target="defaultCost", source="defaultCost")
    @Mapping(target="defaultCurrency", source="defaultCurrency")
    @Mapping(target="active", source="active")
    void apply(ServiceCatalogItemDtos.Request request, @MappingTarget ServiceCatalogItem entity);
}
