package com.codevo.businessos.calendar;

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
public class CalendarEventMapperImpl implements CalendarEventMapper {

    @Override
    public CalendarEventDtos.View view(CalendarEvent entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID clientId = null;
        UUID projectId = null;
        String title = null;
        String type = null;
        Instant startAt = null;
        Instant endAt = null;
        UUID assignedUserId = null;
        String notes = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;

        id = entity.getId();
        clientId = entity.getClientId();
        projectId = entity.getProjectId();
        title = entity.getTitle();
        type = entity.getType();
        startAt = entity.getStartAt();
        endAt = entity.getEndAt();
        assignedUserId = entity.getAssignedUserId();
        notes = entity.getNotes();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();

        CalendarEventDtos.View view = new CalendarEventDtos.View( id, clientId, projectId, title, type, startAt, endAt, assignedUserId, notes, createdAt, updatedAt, version );

        return view;
    }

    @Override
    public void apply(CalendarEventDtos.Request request, CalendarEvent entity) {
        if ( request == null ) {
            return;
        }

        entity.setClientId( request.clientId() );
        entity.setProjectId( request.projectId() );
        entity.setTitle( request.title() );
        entity.setType( request.type() );
        entity.setStartAt( request.startAt() );
        entity.setEndAt( request.endAt() );
        entity.setAssignedUserId( request.assignedUserId() );
        entity.setNotes( request.notes() );
    }
}
