package com.codevo.businessos.project;

import com.codevo.businessos.activity.ActivityService;
import com.codevo.businessos.billing.FinancialSummaryService;
import com.codevo.businessos.common.*;
import com.codevo.businessos.security.Permissions;
import jakarta.validation.constraints.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ProjectCloseoutService {
    public record Review(UUID projectId,long version,String status,long totalTasks,long completedTasks,
        long totalServices,long deliveredServices,long fileCount,long upcomingSubscriptions,
        FinancialSummaryService.Summary financial) {}
    public record Complete(@NotNull Long version,@NotBlank @Size(min=3,max=1000) String reason,
        @AssertTrue boolean reviewed) {}
    private final ProjectRepository projects;
    private final JdbcTemplate jdbc;
    private final FinancialSummaryService financial;
    private final ActivityService activity;
    private final Clock clock;
    public ProjectCloseoutService(ProjectRepository projects,JdbcTemplate jdbc,FinancialSummaryService financial,ActivityService activity,Clock clock) {
        this.projects=projects;this.jdbc=jdbc;this.financial=financial;this.activity=activity;this.clock=clock;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public Review review(UUID id) {
        var project=projects.findById(id).orElseThrow(()->DomainException.missing("Project"));
        var tasks=jdbc.queryForMap("select count(*) total,coalesce(sum(status='DONE'),0) done from tasks where project_id=? and status<>'CANCELLED'",id);
        var services=jdbc.queryForMap("select count(*) total,coalesce(sum(status in ('DELIVERED','COMPLETED')),0) delivered from project_services where project_id=? and status<>'CANCELLED'",id);
        long files=jdbc.queryForObject("select count(*) from file_metadata f where f.project_id=? or f.invoice_id in (select id from invoices where project_id=?) or f.task_id in (select id from tasks where project_id=?)",Long.class,id,id,id);
        boolean finance=Permissions.currentHas("FINANCE_READ");
        long subscriptions=finance?jdbc.queryForObject("select count(*) from subscriptions where project_id=? and status in ('ACTIVE','PAST_DUE','EXPIRED')",Long.class,id):0;
        return new Review(id,project.getVersion(),project.getStatus(),number(tasks,"total"),number(tasks,"done"),number(services,"total"),number(services,"delivered"),files,subscriptions,finance?financial.summary(null,id):null);
    }
    /** A financial risk is visible to finance users but never prevents operational completion. */
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public Review complete(UUID id,Complete request) {
        var project=projects.lockById(id).orElseThrow(()->DomainException.missing("Project"));
        if(project.getVersion()!=request.version())throw DomainException.conflict("Project changed; refresh the closeout review");
        if(!request.reviewed())throw DomainException.invalid("Review the closeout checklist before completing the project");
        if("COMPLETED".equals(project.getStatus()))return review(id);
        if("CANCELLED".equals(project.getStatus()))throw DomainException.conflict("Reopen the cancelled project before completing it");
        var before=review(id);
        project.setStatus("COMPLETED");project.setCompletedAt(clock.instant());projects.flush();
        UUID actor=UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        jdbc.update("insert into project_closeouts(id,project_id,reason,open_tasks,undelivered_services,file_count,actor_user_id) values(?,?,?,?,?,?,?)",UUID.randomUUID(),id,request.reason(),before.totalTasks()-before.completedTasks(),before.totalServices()-before.deliveredServices(),before.fileCount(),actor);
        activity.record("projects",id,project.getClientId(),"COMPLETED","Project closeout: "+request.reason()+"; open tasks "+(before.totalTasks()-before.completedTasks())+", undelivered services "+(before.totalServices()-before.deliveredServices()));
        return review(id);
    }
    private long number(java.util.Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
}
