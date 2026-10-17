package com.codevo.businessos.task;

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
public class TaskMapperImpl implements TaskMapper {

    @Override
    public TaskDtos.View view(Task entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        UUID projectId = null;
        UUID clientId = null;
        String title = null;
        String description = null;
        String status = null;
        String priority = null;
        UUID assignedUserId = null;
        Instant dueAt = null;
        Instant completedAt = null;
        Integer sortOrder = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;

        id = entity.getId();
        projectId = entity.getProjectId();
        clientId = entity.getClientId();
        title = entity.getTitle();
        description = entity.getDescription();
        status = entity.getStatus();
        priority = entity.getPriority();
        assignedUserId = entity.getAssignedUserId();
        dueAt = entity.getDueAt();
        completedAt = entity.getCompletedAt();
        sortOrder = entity.getSortOrder();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();

        TaskDtos.View view = new TaskDtos.View( id, projectId, clientId, title, description, status, priority, assignedUserId, dueAt, completedAt, sortOrder, createdAt, updatedAt, version );

        return view;
    }

    @Override
    public void apply(TaskDtos.Request request, Task entity) {
        if ( request == null ) {
            return;
        }

        entity.setProjectId( request.projectId() );
        entity.setClientId( request.clientId() );
        entity.setTitle( request.title() );
        entity.setDescription( request.description() );
        entity.setStatus( request.status() );
        entity.setPriority( request.priority() );
        entity.setAssignedUserId( request.assignedUserId() );
        entity.setDueAt( request.dueAt() );
        entity.setCompletedAt( request.completedAt() );
        entity.setSortOrder( request.sortOrder() );
    }
}
