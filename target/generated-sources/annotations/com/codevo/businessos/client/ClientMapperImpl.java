package com.codevo.businessos.client;

import com.codevo.businessos.common.Currency;
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
public class ClientMapperImpl implements ClientMapper {

    @Override
    public ClientDtos.View view(Client entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String displayName = null;
        String companyName = null;
        String contactPerson = null;
        String email = null;
        String phone = null;
        String whatsapp = null;
        String country = null;
        String city = null;
        String website = null;
        String source = null;
        String status = null;
        String notes = null;
        Currency preferredCurrency = null;
        String tags = null;
        UUID assignedUserId = null;
        LocalDate firstContactDate = null;
        LocalDate clientSinceDate = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        long version = 0L;
        UUID mergedIntoId = null;
        Instant mergedAt = null;

        id = entity.getId();
        displayName = entity.getDisplayName();
        companyName = entity.getCompanyName();
        contactPerson = entity.getContactPerson();
        email = entity.getEmail();
        phone = entity.getPhone();
        whatsapp = entity.getWhatsapp();
        country = entity.getCountry();
        city = entity.getCity();
        website = entity.getWebsite();
        source = entity.getSource();
        status = entity.getStatus();
        notes = entity.getNotes();
        preferredCurrency = entity.getPreferredCurrency();
        tags = entity.getTags();
        assignedUserId = entity.getAssignedUserId();
        firstContactDate = entity.getFirstContactDate();
        clientSinceDate = entity.getClientSinceDate();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();
        version = entity.getVersion();
        mergedIntoId = entity.getMergedIntoId();
        mergedAt = entity.getMergedAt();

        ClientDtos.View view = new ClientDtos.View( id, displayName, companyName, contactPerson, email, phone, whatsapp, country, city, website, source, status, notes, preferredCurrency, tags, assignedUserId, firstContactDate, clientSinceDate, createdAt, updatedAt, version, mergedIntoId, mergedAt );

        return view;
    }

    @Override
    public void apply(ClientDtos.Request request, Client entity) {
        if ( request == null ) {
            return;
        }

        entity.setDisplayName( request.displayName() );
        entity.setCompanyName( request.companyName() );
        entity.setContactPerson( request.contactPerson() );
        entity.setEmail( request.email() );
        entity.setPhone( request.phone() );
        entity.setWhatsapp( request.whatsapp() );
        entity.setCountry( request.country() );
        entity.setCity( request.city() );
        entity.setWebsite( request.website() );
        entity.setSource( request.source() );
        entity.setStatus( request.status() );
        entity.setNotes( request.notes() );
        entity.setPreferredCurrency( request.preferredCurrency() );
        entity.setTags( request.tags() );
        entity.setAssignedUserId( request.assignedUserId() );
        entity.setFirstContactDate( request.firstContactDate() );
        entity.setClientSinceDate( request.clientSinceDate() );
    }
}
