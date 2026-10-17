package com.codevo.businessos.lead;

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
public class LeadService {
    private final LeadRepository repo;
    private final LeadMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public LeadService(LeadRepository repo, LeadMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public PageResponse<LeadDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("name","stage",Set.of("assignedUserId"),null),filter.pageable(Set.of("createdAt","name","stage","assignedUserId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public LeadDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("Lead"))); }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public LeadDtos.View create(LeadDtos.Request request) {
        var e=new Lead(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("leads",e.getId(),e.getConvertedClientId(),"CREATED","Created Lead: "+e.getName());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public LeadDtos.View update(UUID id, LeadDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Lead"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        if(e.getConvertedClientId()!=null) throw DomainException.conflict("Converted leads are immutable; update the client instead");
        String before=e.getStage()+" / follow-up "+e.getNextFollowUpAt();
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("leads",e.getId(),e.getConvertedClientId(),"UPDATED","Updated Lead: "+before+" → "+e.getStage()+" / follow-up "+e.getNextFollowUpAt());
        return mapper.view(e);
    }
    private void validate(Lead e) {
        refs.user(e.getAssignedUserId()); if(e.getStage().equals("LOST") && (e.getLostReason()==null || e.getLostReason().isBlank())) throw DomainException.invalid("Lost reason is required"); e.setEstimatedValue(Money.amount(e.getEstimatedValue(),e.getCurrency()));
    }
}
