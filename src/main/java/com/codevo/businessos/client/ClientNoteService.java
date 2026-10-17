package com.codevo.businessos.client;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.activity.ActivityService;

@Service
@Transactional(readOnly=true)
public class ClientNoteService {
    private final ClientNoteRepository repo;
    private final ClientNoteMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public ClientNoteService(ClientNoteRepository repo, ClientNoteMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public PageResponse<ClientNoteDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("body",null,Set.of("clientId"),null),filter.pageable(Set.of("createdAt","body","clientId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public ClientNoteDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("ClientNote"))); }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public ClientNoteDtos.View create(ClientNoteDtos.Request request) {
        var e=new ClientNote(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("client_notes",e.getId(),e.getClientId(),"CREATED","Created ClientNote: "+e.getBody());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public ClientNoteDtos.View update(UUID id, ClientNoteDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("ClientNote"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("client_notes",e.getId(),e.getClientId(),"UPDATED","Updated ClientNote: "+e.getBody());
        return mapper.view(e);
    }
    private void validate(ClientNote e) {
        if(e.getClientId()!=null) refs.client(e.getClientId()); if(e.getBody()==null || e.getBody().isBlank()) throw DomainException.invalid("Note cannot be empty");
    }
}
