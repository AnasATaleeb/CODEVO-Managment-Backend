package com.codevo.businessos.asset;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface AssetMapper {
    AssetDtos.View view(Asset entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="projectId", source="projectId")
    @Mapping(target="name", source="name")
    @Mapping(target="type", source="type")
    @Mapping(target="url", source="url")
    @Mapping(target="provider", source="provider")
    @Mapping(target="identifier", source="identifier")
    @Mapping(target="renewalDate", source="renewalDate")
    @Mapping(target="expiryDate", source="expiryDate")
    @Mapping(target="notes", source="notes")
    @Mapping(target="credentialReference", source="credentialReference")
    void apply(AssetDtos.Request request, @MappingTarget Asset entity);
}
