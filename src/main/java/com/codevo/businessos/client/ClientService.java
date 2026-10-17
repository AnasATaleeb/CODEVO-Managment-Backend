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
public class ClientService {
    private final ClientRepository repo;
    private final ClientMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public ClientService(ClientRepository repo, ClientMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public PageResponse<ClientDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.<Client>specification("displayName","status",Set.of("assignedUserId"),null).and((root,query,cb)->cb.isNull(root.get("mergedIntoId"))),filter.pageable(Set.of("createdAt","displayName","status","assignedUserId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public ClientDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("Client"))); }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public ClientDtos.View create(ClientDtos.Request request) {
        var e=new Client(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("clients",e.getId(),e.getId(),"CREATED","Created Client: "+e.getDisplayName());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public ClientDtos.View update(UUID id, ClientDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Client"));
        if(e.getMergedIntoId()!=null) throw DomainException.conflict("Client was merged; use the active client profile");
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        String before=e.getStatus();
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("clients",e.getId(),e.getId(),"UPDATED","Updated Client: "+e.getDisplayName()+" / status "+before+" → "+e.getStatus());
        return mapper.view(e);
    }
    private void validate(Client e) {
        refs.user(e.getAssignedUserId());
    }
}
