package com.codevo.businessos.project;

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
public class ProjectApplicationService {
    private final ProjectRepository repo;
    private final ProjectMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    private final ProjectCommercialReadService commercial;
    private final ProjectServiceApplicationService services;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public ProjectApplicationService(ProjectRepository repo, ProjectMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock,ProjectCommercialReadService commercial,ProjectServiceApplicationService services,org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
        this.commercial=commercial;this.services=services;this.jdbc=jdbc;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<ProjectDtos.View> list(ListFilter filter) {
        var page=repo.findAll(filter.specification("name","status",Set.of("clientId","assignedUserId"),"deadline"),filter.pageable(Set.of("createdAt","name","status","deadline","clientId","assignedUserId")));
        var totals=commercial.projectTotals(page.getContent().stream().map(Project::getId).toList());
        return PageResponse.of(page.map(e->mapper.view(e,totals.getOrDefault(e.getId(),java.util.List.of()))));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public ProjectDtos.View get(UUID id) { return view(repo.findById(id).orElseThrow(()->DomainException.missing("Project"))); }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public ProjectDtos.View create(ProjectDtos.Request request) {
        checkFinancialInput(request);
        if("COMPLETED".equals(request.status()))throw DomainException.invalid("Create the project before reviewing its closeout");
        var e=new Project(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("projects",e.getId(),e.getClientId(),"CREATED","Created Project: "+e.getName());
        if(request.services()!=null) request.services().forEach(input->services.create(input.forProject(e.getId())));
        return view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public ProjectDtos.View update(UUID id, ProjectDtos.Request request) {
        checkFinancialInput(request);
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("Project"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        if(request.services()!=null && !request.services().isEmpty()) throw DomainException.invalid("Update project services using their individual endpoints");
        if(!e.getClientId().equals(request.clientId()) && Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from project_services where project_id=?) or exists(select 1 from invoices where project_id=?) or exists(select 1 from subscriptions where project_id=?) or exists(select 1 from expenses where project_id=?)",Boolean.class,id,id,id,id)))
            throw DomainException.conflict("A project with related commercial records cannot change client");
        var legacyPrice=e.getEstimatedPrice();var legacyCost=e.getEstimatedCost();
        if("COMPLETED".equals(request.status()) && !"COMPLETED".equals(e.getStatus()))throw DomainException.conflict("Complete the project using the closeout review");
        String before=e.getStatus()+" / deadline "+e.getDeadline();
        mapper.apply(request,e);
        if(request.estimatedPrice()==null)e.setEstimatedPrice(legacyPrice);if(request.estimatedCost()==null)e.setEstimatedCost(legacyCost);
        validate(e); repo.flush();
        activity.record("projects",e.getId(),e.getClientId(),"UPDATED","Updated Project: "+before+" → "+e.getStatus()+" / deadline "+e.getDeadline());
        return view(e);
    }
    private ProjectDtos.View view(Project e) { return mapper.view(e,commercial.projectTotals(java.util.List.of(e.getId())).getOrDefault(e.getId(),java.util.List.of())); }
    private void checkFinancialInput(ProjectDtos.Request request) {
        if(request.estimatedPrice()!=null || request.estimatedCost()!=null || (request.services()!=null && !request.services().isEmpty()))
            com.codevo.businessos.security.Permissions.requireFinancialWrite();
    }
    private void validate(Project e) {
        if(e.getClientId()!=null) refs.client(e.getClientId()); refs.user(e.getAssignedUserId()); if(e.getStartDate()!=null && e.getDeadline()!=null && e.getDeadline().isBefore(e.getStartDate())) throw DomainException.invalid("Deadline must follow start date"); e.setCompletedAt(e.getStatus().equals("COMPLETED") ? (e.getCompletedAt()==null?clock.instant():e.getCompletedAt()) : null); e.setEstimatedPrice(Money.amount(e.getEstimatedPrice()==null?BigDecimal.ZERO:e.getEstimatedPrice(),e.getCurrency())); e.setEstimatedCost(Money.amount(e.getEstimatedCost()==null?BigDecimal.ZERO:e.getEstimatedCost(),e.getCurrency()));
    }
}
