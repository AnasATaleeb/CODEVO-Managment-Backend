package com.codevo.businessos.client;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface ClientMapper {
    ClientDtos.View view(Client entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="displayName", source="displayName")
    @Mapping(target="companyName", source="companyName")
    @Mapping(target="contactPerson", source="contactPerson")
    @Mapping(target="email", source="email")
    @Mapping(target="phone", source="phone")
    @Mapping(target="whatsapp", source="whatsapp")
    @Mapping(target="country", source="country")
    @Mapping(target="city", source="city")
    @Mapping(target="website", source="website")
    @Mapping(target="source", source="source")
    @Mapping(target="status", source="status")
    @Mapping(target="notes", source="notes")
    @Mapping(target="preferredCurrency", source="preferredCurrency")
    @Mapping(target="tags", source="tags")
    @Mapping(target="assignedUserId", source="assignedUserId")
    @Mapping(target="firstContactDate", source="firstContactDate")
    @Mapping(target="clientSinceDate", source="clientSinceDate")
    void apply(ClientDtos.Request request, @MappingTarget Client entity);
}
