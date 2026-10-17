package com.codevo.businessos.project;

import com.codevo.businessos.common.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.jdbc.core.JdbcTemplate;
import com.codevo.businessos.activity.ActivityService;

@Service
@Transactional(readOnly=true)
public class ProjectServiceApplicationService {
    private final ProjectServiceRepository repo;
    private final ProjectRepository projects;
    private final ProjectServiceMapper mapper;
    private final ProjectCommercialReadService commercial;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final JdbcTemplate jdbc;
    public ProjectServiceApplicationService(ProjectServiceRepository repo,ProjectRepository projects,ProjectServiceMapper mapper,
        ProjectCommercialReadService commercial,ReferenceChecks refs,ActivityService activity,JdbcTemplate jdbc) {
        this.repo=repo;this.projects=projects;this.mapper=mapper;this.commercial=commercial;this.refs=refs;this.activity=activity;this.jdbc=jdbc;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<ProjectServiceDtos.View> list(ListFilter filter) {
        org.springframework.data.jpa.domain.Specification<ProjectService> spec=filter.specification("customName","status",Set.of("projectId"),"deadline");
        if(filter.getClientId()!=null) spec=spec.and((root,query,cb)->{ var sub=query.subquery(UUID.class);var project=sub.from(Project.class);sub.select(project.get("id")).where(cb.equal(project.get("clientId"),filter.getClientId()));return root.get("projectId").in(sub); });
        var page=repo.findAll(spec,filter.pageable(Set.of("createdAt","customName","status","deadline","projectId")));
        var billed=commercial.billingAmounts(page.getContent().stream().map(ProjectService::getId).toList());
        return PageResponse.of(page.map(e->mapper.view(e,billed.getOrDefault(e.getId(),ProjectCommercialPolicy.BillingAmounts.none()))));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public ProjectServiceDtos.View get(UUID id) { return view(repo.findById(id).orElseThrow(()->DomainException.missing("ProjectService"))); }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public ProjectServiceDtos.View create(ProjectServiceDtos.Request request) {
        checkFinancialInput(request);
        projects.lockById(request.projectId()).orElseThrow(()->DomainException.missing("Project"));
        var e=new ProjectService();mapper.apply(request,e);applyCommercial(request,e);
        if(!com.codevo.businessos.security.Permissions.currentHas("FINANCE_WRITE")) {
            var defaults=jdbc.query("select default_price,default_cost,default_currency from service_catalog where id=?",rs->rs.next()?new Object[]{rs.getBigDecimal(1),rs.getBigDecimal(2),rs.getString(3)}:null,request.serviceCatalogItemId());
            if(defaults==null)throw DomainException.missing("ServiceCatalogItem");
            if(!request.currency().name().equals(defaults[2]))throw new org.springframework.security.access.AccessDeniedException("Financial write permission required to change catalog currency");
            e.setPrice((BigDecimal)defaults[0]);e.setCost((BigDecimal)defaults[1]);
        }
        validate(e);repo.saveAndFlush(e);
        activity.record("project_services",e.getId(),refs.projectClient(e.getProjectId()),"CREATED","Created service: "+e.getCustomName());
        return view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE')")
    public ProjectServiceDtos.View update(UUID id,ProjectServiceDtos.Request request) {
        checkFinancialInput(request);
        var originalProject=jdbc.query("select project_id from project_services where id=?",rs->rs.next()?SqlIds.uuid(rs,1):null,id);
        if(originalProject==null) throw DomainException.missing("ProjectService");
        // All commercial commands acquire the project before its services.
        var projectIds=new TreeSet<UUID>();projectIds.add(originalProject);projectIds.add(request.projectId());
        projectIds.forEach(projectId->projects.lockById(projectId).orElseThrow(()->DomainException.missing("Project")));
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("ProjectService"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        boolean hasHistory=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from invoice_items where project_service_id=?) or exists(select 1 from subscriptions where project_service_id=?) or exists(select 1 from project_billing_milestone_items where project_service_id=?)",Boolean.class,id,id,id));
        if(hasHistory && (!e.getProjectId().equals(request.projectId()) || e.getCurrency()!=request.currency()))
            throw DomainException.conflict("A service with billing history cannot move project or change currency");
        var billed=commercial.currentBillingAmounts(List.of(id)).getOrDefault(id,ProjectCommercialPolicy.BillingAmounts.none());
        var previousPrice=e.getPrice();var previousCost=e.getCost();
        if(!com.codevo.businessos.security.Permissions.currentHas("FINANCE_WRITE") && e.getCurrency()!=request.currency())
            throw new org.springframework.security.access.AccessDeniedException("Financial write permission required to change service currency");
        mapper.apply(request,e);applyCommercial(request,e);
        if(!com.codevo.businessos.security.Permissions.currentHas("FINANCE_WRITE")) { e.setPrice(previousPrice);e.setCost(previousCost); }
        validate(e);
        var agreed=ProjectCommercialPolicy.total(e.getQuantity(),e.getPrice(),e.getCurrency());
        if(agreed.compareTo(billed.invoicedAmount().add(billed.draftReservedAmount()))<0)
            throw DomainException.conflict("Service value cannot fall below issued invoices and reserved drafts");
        BigDecimal scheduled=jdbc.queryForObject("select coalesce(sum(mi.amount),0) from project_billing_milestone_items mi join project_billing_milestones m on m.id=mi.milestone_id where mi.project_service_id=? and m.status='PLANNED'",BigDecimal.class,id);
        if(scheduled.compareTo(agreed)>0 || (scheduled.signum()>0 && "CANCELLED".equals(e.getStatus())))
            throw DomainException.conflict("Cancel or adjust billing milestones before reducing or cancelling the service");
        repo.flush();activity.record("project_services",e.getId(),refs.projectClient(e.getProjectId()),"UPDATED","Updated service: "+e.getCustomName());
        return mapper.view(e,billed);
    }
    private ProjectServiceDtos.View view(ProjectService e) {
        return mapper.view(e,commercial.billingAmounts(List.of(e.getId())).getOrDefault(e.getId(),ProjectCommercialPolicy.BillingAmounts.none()));
    }
    private void applyCommercial(ProjectServiceDtos.Request r,ProjectService e) {
        e.setPrice(alias(r.price(),r.unitPrice(),"unit price"));e.setCost(alias(r.cost(),r.estimatedUnitCost(),"estimated unit cost"));
        String interval=ProjectCommercialPolicy.billingInterval(r.billingType(),r.billingInterval());
        if(interval==null && "CUSTOM".equals(r.billingType()) && r.customIntervalMonths()!=null) interval="CUSTOM";
        if(Set.of("MONTHLY","QUARTERLY","SEMI_ANNUAL","YEARLY").contains(r.billingType()) && !r.billingType().equals(interval))
            throw DomainException.invalid("Billing interval must match the selected recurring billing type");
        e.setBillingInterval(interval);
        if("CUSTOM".equals(interval)) {
            if(r.customIntervalMonths()==null || r.customIntervalMonths()<1 || r.customIntervalMonths()>120)
                throw DomainException.invalid("Custom billing interval requires months between 1 and 120");
        } else if(r.customIntervalMonths()!=null) throw DomainException.invalid("Custom interval months only apply to custom billing");
        if(Set.of("MONTHLY","QUARTERLY","SEMI_ANNUAL","YEARLY").contains(r.billingType())) e.setRecurring(true);
    }
    private void checkFinancialInput(ProjectServiceDtos.Request request) {
        if(request.price()!=null || request.cost()!=null || request.unitPrice()!=null || request.estimatedUnitCost()!=null)
            com.codevo.businessos.security.Permissions.requireFinancialWrite();
    }
    private BigDecimal alias(BigDecimal legacy,BigDecimal preferred,String label) {
        if(legacy!=null && preferred!=null && legacy.compareTo(preferred)!=0) throw DomainException.invalid("Conflicting "+label+" fields");
        return preferred==null?legacy:preferred;
    }
    private void validate(ProjectService e) {
        refs.project(e.getProjectId(),null);refs.service(e.getServiceCatalogItemId());
        e.setPrice(Money.amount(e.getPrice(),e.getCurrency()));e.setCost(Money.amount(e.getCost(),e.getCurrency()));
        if(e.getQuantity()==null || e.getQuantity().signum()<=0) throw DomainException.invalid("Quantity must be positive");
        ProjectCommercialPolicy.total(e.getQuantity(),e.getPrice(),e.getCurrency());ProjectCommercialPolicy.total(e.getQuantity(),e.getCost(),e.getCurrency());
        if(e.getStartDate()!=null && e.getDeliveryDate()!=null && e.getDeliveryDate().isBefore(e.getStartDate())) throw DomainException.invalid("Delivery date must follow start date");
        if(e.getStartDate()!=null && e.getDeadline()!=null && e.getDeadline().isBefore(e.getStartDate())) throw DomainException.invalid("Deadline must follow start date");
    }
}
