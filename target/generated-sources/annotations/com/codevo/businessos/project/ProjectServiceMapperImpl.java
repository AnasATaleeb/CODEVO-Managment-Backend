package com.codevo.businessos.project;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-18T00:38:43+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class ProjectServiceMapperImpl implements ProjectServiceMapper {

    @Override
    public void apply(ProjectServiceDtos.Request request, ProjectService entity) {
        if ( request == null ) {
            return;
        }

        entity.setProjectId( request.projectId() );
        entity.setServiceCatalogItemId( request.serviceCatalogItemId() );
        entity.setCustomName( request.customName() );
        entity.setDescription( request.description() );
        entity.setBillingType( request.billingType() );
        entity.setBillingInterval( request.billingInterval() );
        entity.setCustomIntervalMonths( request.customIntervalMonths() );
        entity.setPrice( request.price() );
        entity.setCost( request.cost() );
        entity.setCurrency( request.currency() );
        entity.setQuantity( request.quantity() );
        entity.setStartDate( request.startDate() );
        entity.setDeliveryDate( request.deliveryDate() );
        entity.setDeadline( request.deadline() );
        entity.setStatus( request.status() );
        entity.setRecurring( request.recurring() );
        entity.setNotes( request.notes() );
    }
}
