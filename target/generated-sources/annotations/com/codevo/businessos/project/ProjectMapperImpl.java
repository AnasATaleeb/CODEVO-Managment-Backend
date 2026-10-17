package com.codevo.businessos.project;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-17T20:33:30+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class ProjectMapperImpl implements ProjectMapper {

    @Override
    public void apply(ProjectDtos.Request request, Project entity) {
        if ( request == null ) {
            return;
        }

        entity.setClientId( request.clientId() );
        entity.setName( request.name() );
        entity.setProjectCode( request.projectCode() );
        entity.setDescription( request.description() );
        entity.setStatus( request.status() );
        entity.setPriority( request.priority() );
        entity.setStartDate( request.startDate() );
        entity.setDeadline( request.deadline() );
        entity.setCompletedAt( request.completedAt() );
        entity.setAssignedUserId( request.assignedUserId() );
        entity.setEstimatedPrice( request.estimatedPrice() );
        entity.setEstimatedCost( request.estimatedCost() );
        entity.setCurrency( request.currency() );
        entity.setNotes( request.notes() );
    }
}
