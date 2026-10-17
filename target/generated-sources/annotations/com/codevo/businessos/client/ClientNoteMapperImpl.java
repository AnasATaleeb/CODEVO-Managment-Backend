package com.codevo.businessos.client;

import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-17T20:33:30+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class ClientNoteMapperImpl implements ClientNoteMapper {

    @Override
    public ClientNoteDtos.View view(ClientNote entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID clientId = null;
        String body = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;

        id = entity.getId();
        clientId = entity.getClientId();
        body = entity.getBody();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();

        ClientNoteDtos.View view = new ClientNoteDtos.View( id, clientId, body, createdAt, updatedAt, version );

        return view;
    }

    @Override
    public void apply(ClientNoteDtos.Request request, ClientNote entity) {
        if ( request == null ) {
            return;
        }

        entity.setClientId( request.clientId() );
        entity.setBody( request.body() );
    }
}
