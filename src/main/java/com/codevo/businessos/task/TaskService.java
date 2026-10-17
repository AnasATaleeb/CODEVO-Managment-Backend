package com.codevo.businessos.task;

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
public class TaskService {
    private final TaskRepository repo;
    private final TaskMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public TaskService(TaskRepository repo, TaskMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<TaskDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("title","status",Set.of("clientId","projectId","assignedUserId"),"dueAt"),filter.pageable(Set.of("createdAt","title","status","dueAt","clientId","projectId","assignedUserId"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public TaskDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("Task"))); }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public TaskDtos.View create(TaskDtos.Request request) {
        var e=new Task(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("tasks",e.getId(),e.getClientId(),"CREATED","Created Task: "+e.getTitle());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public TaskDtos.View update(UUID id, TaskDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Task"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        String before=e.getStatus()+" / deadline "+e.getDueAt();
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("tasks",e.getId(),e.getClientId(),"UPDATED","Updated Task: "+before+" → "+e.getStatus()+" / deadline "+e.getDueAt());
        return mapper.view(e);
    }
    private void validate(Task e) {
        if(e.getClientId()!=null) refs.client(e.getClientId()); refs.project(e.getProjectId(),e.getClientId()); refs.user(e.getAssignedUserId()); if(e.getProjectId()==null && e.getClientId()==null) throw DomainException.invalid("Task needs a project or client"); if(e.getProjectId()!=null && e.getClientId()==null) e.setClientId(refs.projectClient(e.getProjectId())); e.setCompletedAt(e.getStatus().equals("DONE") ? (e.getCompletedAt()==null?clock.instant():e.getCompletedAt()) : null);
    }
}
