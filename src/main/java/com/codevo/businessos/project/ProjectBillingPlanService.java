package com.codevo.businessos.project;

import com.codevo.businessos.activity.ActivityService;
import com.codevo.businessos.billing.invoice.InvoiceDtos;
import com.codevo.businessos.billing.invoice.InvoiceService;
import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A billing schedule references service agreements; only invoices enter the ledger. */
@Service @Transactional(readOnly=true)
public class ProjectBillingPlanService {
    public record Selection(@NotNull UUID projectServiceId,
        @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=3) BigDecimal amount,
        @DecimalMin(value="0",inclusive=false) @DecimalMax("100") @Digits(integer=3,fraction=3) BigDecimal percentage) {}
    public record Create(@NotBlank @Size(max=255) String name,@NotNull Currency currency,
        @NotNull LocalDate dueDate,@Size(max=255) String triggerLabel,
        @NotEmpty @Size(max=100) List<@Valid Selection> services) {}
    public record Generate(@NotNull Long version,@NotNull LocalDate issueDate,
        @NotNull @Pattern(regexp="DRAFT|ISSUED") String status) {}
    public record Cancel(@NotNull Long version,@NotBlank @Size(min=3,max=1000) String reason) {}
    public record Item(UUID projectServiceId,String name,BigDecimal amount) {}
    public record View(UUID id,String name,Currency currency,LocalDate dueDate,String triggerLabel,
        String status,long version,BigDecimal amount,UUID invoiceId,String invoiceNumber,String invoiceStatus,List<Item> services) {}
    private final JdbcTemplate jdbc;
    private final ProjectRepository projects;
    private final ProjectServiceRepository services;
    private final InvoiceService invoices;
    private final ActivityService activity;
    private final Clock clock;
    public ProjectBillingPlanService(JdbcTemplate jdbc,ProjectRepository projects,ProjectServiceRepository services,
        InvoiceService invoices,ActivityService activity,Clock clock) {
        this.jdbc=jdbc;this.projects=projects;this.services=services;this.invoices=invoices;this.activity=activity;this.clock=clock;
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public List<View> list(UUID projectId) {
        projects.findById(projectId).orElseThrow(()->DomainException.missing("Project"));
        var items=jdbc.query("select mi.milestone_id,mi.project_service_id,ps.custom_name,mi.amount from project_billing_milestone_items mi join project_services ps on ps.id=mi.project_service_id join project_billing_milestones m on m.id=mi.milestone_id where m.project_id=?",
            (rs,n)->Map.entry(SqlIds.uuid(rs,1),new Item(SqlIds.uuid(rs,2),rs.getString(3),rs.getBigDecimal(4))),projectId);
        Map<UUID,List<Item>> grouped=new HashMap<>();items.forEach(i->grouped.computeIfAbsent(i.getKey(),k->new ArrayList<>()).add(i.getValue()));
        return jdbc.query("select m.*,i.id invoice_id,i.invoice_number,i.status invoice_status from project_billing_milestones m left join project_billing_milestone_invoices link on link.milestone_id=m.id and link.retired_at is null left join invoices i on i.id=link.invoice_id where m.project_id=? order by m.due_date,m.created_at,m.id",(rs,n)->{
            UUID id=SqlIds.uuid(rs,"id");var lines=grouped.getOrDefault(id,List.of());
            return new View(id,rs.getString("name"),Currency.valueOf(rs.getString("currency")),rs.getDate("due_date").toLocalDate(),rs.getString("trigger_label"),rs.getString("status"),rs.getLong("version"),lines.stream().map(Item::amount).reduce(BigDecimal.ZERO,BigDecimal::add),SqlIds.uuid(rs,"invoice_id"),rs.getString("invoice_number"),rs.getString("invoice_status"),lines);
        },projectId);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public View create(UUID projectId,Create request) {
        var project=projects.lockById(projectId).orElseThrow(()->DomainException.missing("Project"));
        if("CANCELLED".equals(project.getStatus()))throw DomainException.conflict("Cancelled projects cannot have new billing milestones");
        Map<UUID,Selection> selected=new TreeMap<>();
        for(var line:request.services())if(selected.put(line.projectServiceId(),line)!=null)throw DomainException.invalid("Select each service once");
        List<Item> lines=new ArrayList<>();
        for(var entry:selected.entrySet()) {
            var service=services.lockById(entry.getKey()).orElseThrow(()->DomainException.missing("Project service"));
            if(!service.getProjectId().equals(projectId)||service.getCurrency()!=request.currency())throw DomainException.invalid("Choose services from this project in the milestone currency");
            if("CANCELLED".equals(service.getStatus()))throw DomainException.conflict("Cancelled services cannot be scheduled");
            var input=entry.getValue();
            if((input.amount()==null)==(input.percentage()==null))throw DomainException.invalid("Choose an amount or percentage for each service");
            var agreed=ProjectCommercialPolicy.total(service.getQuantity(),service.getPrice(),service.getCurrency());
            var amount=input.amount()!=null?Money.amount(input.amount(),request.currency()):Money.rounded(agreed.multiply(input.percentage()).movePointLeft(2),request.currency());
            if(amount.signum()<=0)throw DomainException.invalid("Milestone amount must be positive");
            BigDecimal scheduled=jdbc.queryForObject("select coalesce(sum(mi.amount),0) from project_billing_milestone_items mi join project_billing_milestones m on m.id=mi.milestone_id where mi.project_service_id=? and m.status='PLANNED'",BigDecimal.class,service.getId());
            BigDecimal outsidePlan=jdbc.queryForObject("select coalesce(sum(ii.line_total),0) from invoice_items ii join invoices i on i.id=ii.invoice_id where ii.project_service_id=? and ii.commercial_billing=true and i.status not in ('VOID','CANCELLED') and not exists(select 1 from project_billing_milestone_invoices link where link.invoice_id=i.id)",BigDecimal.class,service.getId());
            if(scheduled.add(outsidePlan).add(amount).compareTo(agreed)>0)throw DomainException.conflict("Billing plan plus existing invoices exceeds the service agreement");
            lines.add(new Item(service.getId(),service.getCustomName(),amount));
        }
        UUID id=UUID.randomUUID();
        jdbc.update("insert into project_billing_milestones(id,project_id,name,currency,due_date,trigger_label) values(?,?,?,?,?,?)",id,projectId,request.name(),request.currency().name(),request.dueDate(),request.triggerLabel());
        lines.forEach(line->jdbc.update("insert into project_billing_milestone_items(milestone_id,project_service_id,amount) values(?,?,?)",id,line.projectServiceId(),line.amount()));
        activity.record("billing_plans",projectId,project.getClientId(),"MILESTONE_CREATED","Billing milestone planned: "+request.name()+"; no invoice or payment recorded");
        return find(projectId,id);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View generate(UUID projectId,UUID id,Generate request) {
        var project=projects.lockById(projectId).orElseThrow(()->DomainException.missing("Project"));
        var milestone=lock(projectId,id,request.version());
        if("CANCELLED".equals(milestone.get("status")))throw DomainException.conflict("Cancelled milestone cannot be invoiced");
        UUID existing=jdbc.query("select invoice_id from project_billing_milestone_invoices where milestone_id=? and retired_at is null",rs->rs.next()?SqlIds.uuid(rs,1):null,id);
        if(existing!=null) {
            var invoice=invoices.get(existing);
            if(!Set.of("VOID","CANCELLED").contains(invoice.status()))return invoice;
            jdbc.update("update project_billing_milestone_invoices set retired_at=? where invoice_id=?",java.sql.Timestamp.from(clock.instant()),existing);
        }
        var view=find(projectId,id);
        var invoice=invoices.createFromProject(projectId,new InvoiceDtos.FromProjectRequest(request.status(),request.issueDate(),view.dueDate(),view.currency(),BigDecimal.ZERO,BigDecimal.ZERO,"Billing milestone: "+view.name(),view.services().stream().map(i->new InvoiceDtos.ServiceSelection(i.projectServiceId(),i.amount(),null)).toList()));
        jdbc.update("insert into project_billing_milestone_invoices(id,milestone_id,invoice_id) values(?,?,?)",UUID.randomUUID(),id,invoice.id());
        activity.record("invoices",invoice.id(),project.getClientId(),"MILESTONE_INVOICED","Milestone "+view.name()+" generated "+invoice.invoiceNumber());
        return invoice;
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public View cancel(UUID projectId,UUID id,Cancel request) {
        var project=projects.lockById(projectId).orElseThrow(()->DomainException.missing("Project"));
        var milestone=lock(projectId,id,request.version());
        if("CANCELLED".equals(milestone.get("status")))throw DomainException.conflict("Milestone already cancelled");
        var view=find(projectId,id);
        if(view.invoiceId()!=null&&!Set.of("VOID","CANCELLED").contains(view.invoiceStatus()))throw DomainException.conflict("Void the linked invoice before cancelling this milestone");
        jdbc.update("update project_billing_milestones set status='CANCELLED',cancellation_reason=?,version=version+1 where id=?",request.reason(),id);
        activity.record("billing_plans",projectId,project.getClientId(),"MILESTONE_CANCELLED","Cancelled milestone "+view.name()+": "+request.reason());
        return find(projectId,id);
    }
    private Map<String,Object> lock(UUID projectId,UUID id,long version) {
        var rows=jdbc.queryForList("select * from project_billing_milestones where id=? and project_id=? for update",id,projectId);
        if(rows.isEmpty())throw DomainException.missing("Billing milestone");
        if(((Number)rows.getFirst().get("version")).longValue()!=version)throw DomainException.conflict("Milestone changed; refresh before continuing");
        return rows.getFirst();
    }
    private View find(UUID projectId,UUID id) { return list(projectId).stream().filter(v->v.id().equals(id)).findFirst().orElseThrow(()->DomainException.missing("Billing milestone")); }
}
