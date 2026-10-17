package com.codevo.businessos.billing.invoice;

import java.util.*;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.common.*;
import com.codevo.businessos.activity.ActivityService;
import com.codevo.businessos.project.*;

@Service @Transactional(readOnly=true)
public class InvoiceService {
    private final InvoiceRepository invoices;private final InvoiceItemRepository items;private final JdbcTemplate jdbc;
    private final ProjectRepository projects;private final ProjectServiceRepository projectServices;private final ProjectCommercialReadService commercial;
    private final ReferenceChecks refs;private final ActivityService activity;private final Clock clock; private final BusinessDates dates;
    public InvoiceService(InvoiceRepository invoices,InvoiceItemRepository items,JdbcTemplate jdbc,ReferenceChecks refs,ActivityService activity,Clock clock, BusinessDates dates,ProjectRepository projects,ProjectServiceRepository projectServices,ProjectCommercialReadService commercial) {
        this.invoices=invoices;this.items=items;this.jdbc=jdbc;this.refs=refs;this.activity=activity;this.clock=clock; this.dates=dates;this.projects=projects;this.projectServices=projectServices;this.commercial=commercial;
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PageResponse<InvoiceDtos.View> list(ListFilter filter) {
        return PageResponse.of(invoices.findAll(filter.specification("invoiceNumber","status",Set.of("clientId","projectId"),"issueDate"),
            filter.pageable(Set.of("createdAt","invoiceNumber","dueDate","issueDate","status","total","balanceDue"))).map(e->view(e,List.of())));
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public InvoiceDtos.View get(UUID id) { return view(invoices.findById(id).orElseThrow(()->DomainException.missing("Invoice")),items.findByInvoiceId(id)); }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View create(InvoiceDtos.Request r) { return createSnapshot(r,true); }

    /** Internal renewal boundary. The subscription module owns a locked, persisted billing period. */
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View createRenewalInvoice(InvoiceDtos.Request r) { return createSnapshot(r,false); }

    /** Trusted local scheduler module API. Not exposed through HTTP; caller locks the persisted subscription period. */
    @Transactional
    public InvoiceDtos.View createScheduledRenewalInvoice(InvoiceDtos.Request r) { return createSnapshot(r,false); }
    /** Internal snapshot lookup for the local scheduler's locked renewal generation. */
    public InvoiceDtos.View scheduledRenewalSnapshot(UUID id) { return view(invoices.findById(id).orElseThrow(()->DomainException.missing("Invoice")),items.findByInvoiceId(id)); }

    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View createFromProject(UUID projectId,InvoiceDtos.FromProjectRequest r) {
        var project=projects.lockById(projectId).orElseThrow(()->DomainException.missing("Project"));
        var selected=lockServices(projectId,r.services().stream().map(InvoiceDtos.ServiceSelection::projectServiceId).toList(),r.currency(),false);
        if(selected.size()!=r.services().size()) throw DomainException.invalid("Select each project service only once");
        var billed=commercial.currentBillingAmounts(selected.keySet());
        List<InvoiceDtos.ItemRequest> lines=new ArrayList<>();
        for(var selection:r.services()) {
            var source=selected.get(selection.projectServiceId());
            if(selection.amount()!=null && selection.quantity()!=null) throw DomainException.invalid("Choose a partial amount or quantity, not both");
            var agreed=ProjectCommercialPolicy.total(source.getQuantity(),source.getPrice(),source.getCurrency());
            var billing=billed.getOrDefault(source.getId(),ProjectCommercialPolicy.BillingAmounts.none());
            BigDecimal amount;BigDecimal quantity;BigDecimal unitPrice;
            if(selection.quantity()!=null) {
                quantity=selection.quantity();unitPrice=source.getPrice();amount=ProjectCommercialPolicy.total(quantity,unitPrice,source.getCurrency());
            } else {
                amount=selection.amount()==null?ProjectCommercialPolicy.remaining(agreed,billing,true):Money.amount(selection.amount(),r.currency());
                // Full service lines retain the agreement's quantity/unit price; a partial monetary milestone is an explicit single line snapshot.
                if(amount.compareTo(agreed)==0) {quantity=source.getQuantity();unitPrice=source.getPrice();}
                else {quantity=BigDecimal.ONE;unitPrice=amount;}
            }
            ProjectCommercialPolicy.requireAvailable(agreed,billing,amount);
            lines.add(new InvoiceDtos.ItemRequest(source.getId(),source.getCustomName(),quantity,unitPrice));
        }
        return createSnapshot(new InvoiceDtos.Request(project.getClientId(),projectId,r.status(),r.issueDate(),r.dueDate(),r.currency(),r.discount(),r.tax(),r.notes(),lines),true);
    }

    private InvoiceDtos.View createSnapshot(InvoiceDtos.Request r,boolean commercialBilling) {
        if(r.projectId()!=null)projects.lockById(r.projectId()).orElseThrow(()->DomainException.missing("Project"));
        var selected=lockServices(r.projectId(),r.items().stream().map(InvoiceDtos.ItemRequest::projectServiceId).filter(Objects::nonNull).toList(),r.currency(),!commercialBilling);
        var billed=commercial.currentBillingAmounts(selected.keySet());
        Map<UUID,BigDecimal> requested=new HashMap<>();
        refs.client(r.clientId());refs.project(r.projectId(),r.clientId());
        if(r.dueDate().isBefore(r.issueDate())) throw DomainException.invalid("Due date must follow issue date");
        Invoice e=new Invoice();e.setClientId(r.clientId());e.setProjectId(r.projectId());e.setCurrency(r.currency());
        var billingClient=jdbc.queryForMap("select display_name,email from clients where id=?",r.clientId());
        e.setOriginalClientId(r.clientId());e.setBillingClientName((String)billingClient.get("display_name"));e.setBillingClientEmail((String)billingClient.get("email"));
        e.setIssueDate(r.issueDate());e.setDueDate(r.dueDate());e.setNotes(r.notes());e.setStatus(r.status());
        e.setDiscount(Money.amount(r.discount(),r.currency()));e.setTax(Money.amount(r.tax(),r.currency()));
        long number=jdbc.queryForObject("select next_value from invoice_counter where id=1 for update",Long.class);
        jdbc.update("update invoice_counter set next_value=next_value+1 where id=1");
        String prefix=jdbc.queryForObject("select invoice_prefix from organization_settings where id=1",String.class);
        e.setInvoiceNumber(prefix+"-"+r.issueDate().getYear()+"-"+String.format("%05d",number));
        List<InvoiceItem> lines=new ArrayList<>();
        for(var line:r.items()) {
            refs.projectService(line.projectServiceId(),r.projectId());
            InvoiceItem item=new InvoiceItem();item.setInvoiceId(e.getId());item.setProjectServiceId(line.projectServiceId());
            item.setDescription(line.description());item.setQuantity(line.quantity());item.setUnitPrice(Money.amount(line.unitPrice(),r.currency()));
            item.setLineTotal(ProjectCommercialPolicy.total(item.getQuantity(),item.getUnitPrice(),r.currency()));item.setCommercialBilling(commercialBilling);lines.add(item);
            if(commercialBilling && line.projectServiceId()!=null)requested.merge(line.projectServiceId(),item.getLineTotal(),BigDecimal::add);
        }
        requested.forEach((serviceId,amount)->{
            var source=selected.get(serviceId);var agreed=ProjectCommercialPolicy.total(source.getQuantity(),source.getPrice(),source.getCurrency());
            ProjectCommercialPolicy.requireAvailable(agreed,billed.getOrDefault(serviceId,ProjectCommercialPolicy.BillingAmounts.none()),amount);
        });
        e.setSubtotal(Money.amount(lines.stream().map(InvoiceItem::getLineTotal).reduce(BigDecimal.ZERO,BigDecimal::add),r.currency()));
        if(e.getDiscount().compareTo(e.getSubtotal())>0) throw DomainException.invalid("Discount cannot exceed subtotal");
        e.setTotal(Money.amount(e.getSubtotal().subtract(e.getDiscount()).add(e.getTax()),r.currency()));
        if(e.getTotal().signum()<=0) throw DomainException.invalid("Invoice total must be positive");
        e.setBalanceDue(e.getTotal());
        if(!e.getStatus().equals("DRAFT")) e.setStatus(InvoicePolicy.status(e.getTotal(),BigDecimal.ZERO,e.getDueDate(),dates.today()));
        invoices.saveAndFlush(e);items.saveAllAndFlush(lines);
        activity.record("invoices",e.getId(),e.getClientId(),"CREATED","Invoice "+e.getInvoiceNumber()+" created");
        return view(e,lines);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View issue(UUID id) {
        lockCommercialParents(id);
        var e=lockForAllocation(id,false);
        if(!e.getStatus().equals("DRAFT")) throw DomainException.conflict("Only draft invoices can be issued");
        e.setStatus(InvoicePolicy.status(e.getTotal(),e.getPaidAmount(),e.getDueDate(),dates.today()));invoices.flush();
        activity.record("invoices",id,e.getClientId(),"ISSUED","Issued "+e.getInvoiceNumber());return view(e,items.findByInvoiceId(id));
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public InvoiceDtos.View voidInvoice(UUID id,String reason) {
        lockCommercialParents(id);
        var e=lockForAllocation(id,false);
        if(Set.of("VOID","CANCELLED").contains(e.getStatus())) throw DomainException.conflict("Invoice already void");
        if(e.getPaidAmount().signum()>0 || e.getPendingCheckAmount().signum()>0) throw DomainException.conflict("Reverse payments before voiding an invoice");
        e.setStatus("VOID");e.setVoidReason(reason);invoices.flush();
        activity.record("invoices",id,e.getClientId(),"VOID","Voided "+e.getInvoiceNumber()+": "+reason);return view(e,items.findByInvoiceId(id));
    }
    /** Internal module API: caller owns a transaction and locks the invoice before payments/checks. */
    public Invoice lockForAllocation(UUID id,boolean payable) {
        var e=invoices.lockById(id).orElseThrow(()->DomainException.missing("Invoice"));
        if(payable && Set.of("DRAFT","VOID","CANCELLED").contains(e.getStatus())) throw DomainException.conflict("Invoice is not payable");
        return e;
    }
    public void recalculate(Invoice invoice) {
        var sums=jdbc.queryForMap("select coalesce(sum(case when collectible then allocation_amount else 0 end),0) collected, coalesce(sum(case when pending then allocation_amount else 0 end),0) pending from ledger_parts where invoice_id=?",invoice.getId());
        BigDecimal collected=(BigDecimal)sums.get("collected"),pending=(BigDecimal)sums.get("pending");
        InvoicePolicy.coverage(invoice.getTotal(),collected,pending);
        invoice.setPaidAmount(collected);invoice.setPendingCheckAmount(pending);invoice.setBalanceDue(invoice.getTotal().subtract(collected));
        String old=invoice.getStatus();
        invoice.setStatus(InvoicePolicy.status(invoice.getTotal(),collected,invoice.getDueDate(),dates.today()));invoices.flush();
        if(!old.equals(invoice.getStatus())) activity.record("invoices",invoice.getId(),invoice.getClientId(),"STATUS_CHANGED","Invoice "+invoice.getInvoiceNumber()+": "+old+" → "+invoice.getStatus());
    }
    private Map<UUID,ProjectService> lockServices(UUID projectId,List<UUID> ids,com.codevo.businessos.common.Currency currency,boolean renewal) {
        Map<UUID,ProjectService> selected=new LinkedHashMap<>();
        for(UUID id:new TreeSet<>(ids)) {
            var source=projectServices.lockById(id).orElseThrow(()->DomainException.missing("Project service"));
            if(!source.getProjectId().equals(projectId)) throw DomainException.invalid("Service does not belong to this project");
            if(source.getCurrency()!=currency) throw DomainException.invalid("Invoice currency must match every selected service");
            if("CANCELLED".equals(source.getStatus())) throw DomainException.conflict("Cancelled services cannot be invoiced");
            if(renewal && !ProjectCommercialPolicy.subscriptionEligible(source)) throw DomainException.conflict("Renewal invoice requires a recurring eligible service");
            selected.put(id,source);
        }
        return selected;
    }
    private void lockCommercialParents(UUID invoiceId) {
        UUID projectId=jdbc.query("select project_id from invoices where id=?",rs->rs.next()?SqlIds.uuid(rs,1):null,invoiceId);
        if(projectId!=null) projects.lockById(projectId).orElseThrow(()->DomainException.missing("Project"));
        var serviceIds=jdbc.query("select distinct project_service_id from invoice_items where invoice_id=? and project_service_id is not null",(rs,row)->SqlIds.uuid(rs,1),invoiceId);
        for(UUID id:new TreeSet<>(serviceIds))projectServices.lockById(id).orElseThrow(()->DomainException.missing("Project service"));
    }
    private InvoiceDtos.View view(Invoice e,List<InvoiceItem> lines) {
        return new InvoiceDtos.View(e.getId(),e.getClientId(),e.getProjectId(),e.getInvoiceNumber(),e.getStatus(),e.getIssueDate(),e.getDueDate(),e.getCurrency(),
            e.getSubtotal(),e.getDiscount(),e.getTax(),e.getTotal(),e.getPaidAmount(),e.getBalanceDue(),e.getPendingCheckAmount(),e.getNotes(),
            lines.stream().map(i->new InvoiceDtos.ItemView(i.getId(),i.getProjectServiceId(),i.getDescription(),i.getQuantity(),i.getUnitPrice(),i.getLineTotal())).toList(),e.getCreatedAt(),e.getVersion(),e.getOriginalClientId(),e.getBillingClientName(),e.getBillingClientEmail());
    }
}
