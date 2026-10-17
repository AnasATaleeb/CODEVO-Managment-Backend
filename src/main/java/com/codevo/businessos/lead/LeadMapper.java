package com.codevo.businessos.lead;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface LeadMapper {
    LeadDtos.View view(Lead entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="name", source="name")
    @Mapping(target="companyName", source="companyName")
    @Mapping(target="phone", source="phone")
    @Mapping(target="whatsapp", source="whatsapp")
    @Mapping(target="email", source="email")
    @Mapping(target="source", source="source")
    @Mapping(target="serviceInterest", source="serviceInterest")
    @Mapping(target="estimatedValue", source="estimatedValue")
    @Mapping(target="currency", source="currency")
    @Mapping(target="stage", source="stage")
    @Mapping(target="priority", source="priority")
    @Mapping(target="nextFollowUpAt", source="nextFollowUpAt")
    @Mapping(target="assignedUserId", source="assignedUserId")
    @Mapping(target="notes", source="notes")
    @Mapping(target="lostReason", source="lostReason")
    void apply(LeadDtos.Request request, @MappingTarget Lead entity);
}
