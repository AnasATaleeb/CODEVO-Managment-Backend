package com.codevo.businessos.lead;

import com.codevo.businessos.common.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-18T00:38:43+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class LeadMapperImpl implements LeadMapper {

    @Override
    public LeadDtos.View view(Lead entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String companyName = null;
        String phone = null;
        String whatsapp = null;
        String email = null;
        String source = null;
        String serviceInterest = null;
        BigDecimal estimatedValue = null;
        Currency currency = null;
        String stage = null;
        String priority = null;
        Instant nextFollowUpAt = null;
        UUID assignedUserId = null;
        String notes = null;
        String lostReason = null;
        UUID convertedClientId = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;

        id = entity.getId();
        name = entity.getName();
        companyName = entity.getCompanyName();
        phone = entity.getPhone();
        whatsapp = entity.getWhatsapp();
        email = entity.getEmail();
        source = entity.getSource();
        serviceInterest = entity.getServiceInterest();
        estimatedValue = entity.getEstimatedValue();
        currency = entity.getCurrency();
        stage = entity.getStage();
        priority = entity.getPriority();
        nextFollowUpAt = entity.getNextFollowUpAt();
        assignedUserId = entity.getAssignedUserId();
        notes = entity.getNotes();
        lostReason = entity.getLostReason();
        convertedClientId = entity.getConvertedClientId();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();

        LeadDtos.View view = new LeadDtos.View( id, name, companyName, phone, whatsapp, email, source, serviceInterest, estimatedValue, currency, stage, priority, nextFollowUpAt, assignedUserId, notes, lostReason, convertedClientId, createdAt, updatedAt, version );

        return view;
    }

    @Override
    public void apply(LeadDtos.Request request, Lead entity) {
        if ( request == null ) {
            return;
        }

        entity.setName( request.name() );
        entity.setCompanyName( request.companyName() );
        entity.setPhone( request.phone() );
        entity.setWhatsapp( request.whatsapp() );
        entity.setEmail( request.email() );
        entity.setSource( request.source() );
        entity.setServiceInterest( request.serviceInterest() );
        entity.setEstimatedValue( request.estimatedValue() );
        entity.setCurrency( request.currency() );
        entity.setStage( request.stage() );
        entity.setPriority( request.priority() );
        entity.setNextFollowUpAt( request.nextFollowUpAt() );
        entity.setAssignedUserId( request.assignedUserId() );
        entity.setNotes( request.notes() );
        entity.setLostReason( request.lostReason() );
    }
}
