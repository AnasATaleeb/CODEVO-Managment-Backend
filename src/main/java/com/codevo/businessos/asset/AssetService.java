package com.codevo.businessos.asset;

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
public class AssetService {
    private final AssetRepository repo;
    private final AssetMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public AssetService(AssetRepository repo, AssetMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<AssetDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("name","type",Set.of("clientId","projectId"),"expiryDate"),filter.pageable(Set.of("createdAt","name","type","expiryDate","clientId","projectId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public AssetDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("Asset"))); }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public AssetDtos.View create(AssetDtos.Request request) {
        var e=new Asset(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("assets",e.getId(),e.getClientId(),"CREATED","Created Asset: "+e.getName());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public AssetDtos.View update(UUID id, AssetDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Asset"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("assets",e.getId(),e.getClientId(),"UPDATED","Updated Asset: "+e.getName());
        return mapper.view(e);
    }
    private void validate(Asset e) {
        if(e.getClientId()!=null) refs.client(e.getClientId()); refs.project(e.getProjectId(),e.getClientId());
    }
}
