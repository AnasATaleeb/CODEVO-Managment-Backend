package com.codevo.businessos.asset;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-18T00:38:43+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class AssetMapperImpl implements AssetMapper {

    @Override
    public AssetDtos.View view(Asset entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID clientId = null;
        UUID projectId = null;
        String name = null;
        String type = null;
        String url = null;
        String provider = null;
        String identifier = null;
        LocalDate renewalDate = null;
        LocalDate expiryDate = null;
        String notes = null;
        String credentialReference = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;

        id = entity.getId();
        clientId = entity.getClientId();
        projectId = entity.getProjectId();
        name = entity.getName();
        type = entity.getType();
        url = entity.getUrl();
        provider = entity.getProvider();
        identifier = entity.getIdentifier();
        renewalDate = entity.getRenewalDate();
        expiryDate = entity.getExpiryDate();
        notes = entity.getNotes();
        credentialReference = entity.getCredentialReference();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();

        AssetDtos.View view = new AssetDtos.View( id, clientId, projectId, name, type, url, provider, identifier, renewalDate, expiryDate, notes, credentialReference, createdAt, updatedAt, version );

        return view;
    }

    @Override
    public void apply(AssetDtos.Request request, Asset entity) {
        if ( request == null ) {
            return;
        }

        entity.setClientId( request.clientId() );
        entity.setProjectId( request.projectId() );
        entity.setName( request.name() );
        entity.setType( request.type() );
        entity.setUrl( request.url() );
        entity.setProvider( request.provider() );
        entity.setIdentifier( request.identifier() );
        entity.setRenewalDate( request.renewalDate() );
        entity.setExpiryDate( request.expiryDate() );
        entity.setNotes( request.notes() );
        entity.setCredentialReference( request.credentialReference() );
    }
}
