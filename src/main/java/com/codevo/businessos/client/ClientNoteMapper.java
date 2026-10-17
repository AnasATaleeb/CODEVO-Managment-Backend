package com.codevo.businessos.client;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface ClientNoteMapper {
    ClientNoteDtos.View view(ClientNote entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="body", source="body")
    void apply(ClientNoteDtos.Request request, @MappingTarget ClientNote entity);
}
