package com.codevo.businessos.calendar;

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
public class CalendarEventService {
    private final CalendarEventRepository repo;
    private final CalendarEventMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public CalendarEventService(CalendarEventRepository repo, CalendarEventMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<CalendarEventDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("title","type",Set.of("clientId","projectId","assignedUserId"),"startAt"),filter.pageable(Set.of("createdAt","title","type","startAt","clientId","projectId","assignedUserId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public CalendarEventDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("CalendarEvent"))); }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public CalendarEventDtos.View create(CalendarEventDtos.Request request) {
        var e=new CalendarEvent(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("calendar_events",e.getId(),e.getClientId(),"CREATED","Created CalendarEvent: "+e.getTitle());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public CalendarEventDtos.View update(UUID id, CalendarEventDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("CalendarEvent"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("calendar_events",e.getId(),e.getClientId(),"UPDATED","Updated CalendarEvent: "+e.getTitle());
        return mapper.view(e);
    }
    private void validate(CalendarEvent e) {
        if(e.getClientId()!=null) refs.client(e.getClientId()); refs.project(e.getProjectId(),e.getClientId()); refs.user(e.getAssignedUserId()); if(e.getStartAt()==null || e.getEndAt()==null || !e.getEndAt().isAfter(e.getStartAt())) throw DomainException.invalid("End time must follow start time");
    }
}
