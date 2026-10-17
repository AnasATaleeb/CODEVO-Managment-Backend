package com.codevo.businessos.billing.payment;

import java.util.*;
import java.util.stream.Collectors;
import java.time.*;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import com.codevo.businessos.billing.invoice.*;
import com.codevo.businessos.activity.ActivityService;

@Service @Transactional(readOnly=true)
public class PaymentService {
    private final PaymentRepository payments;private final PaymentPartRepository parts;private final CheckPaymentRepository checks;
    private final InvoiceService invoices;private final ReferenceChecks refs;private final ActivityService activity;private final JdbcTemplate jdbc;private final Clock clock; private final BusinessDates dates;
    public PaymentService(PaymentRepository payments,PaymentPartRepository parts,CheckPaymentRepository checks,InvoiceService invoices,
        ReferenceChecks refs,ActivityService activity,JdbcTemplate jdbc,Clock clock, BusinessDates dates) {
        this.payments=payments;this.parts=parts;this.checks=checks;this.invoices=invoices;this.refs=refs;this.activity=activity;this.jdbc=jdbc;this.clock=clock; this.dates=dates;
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PageResponse<PaymentDtos.View> list(ListFilter filter) {
        org.springframework.data.jpa.domain.Specification<Payment> spec=filter.specification("notes","status",Set.of("clientId"),"paymentDate");
        if(filter.getProjectId()!=null) spec=spec.and((root,query,cb)->{ var sub=query.subquery(UUID.class); var i=sub.from(Invoice.class);sub.select(i.get("id")).where(cb.equal(i.get("projectId"),filter.getProjectId()));return root.get("invoiceId").in(sub); });
        var page=payments.findAll(spec,
            filter.pageable(Set.of("createdAt","paymentDate","status")));
        var mapped=views(page.getContent());return new PageResponse<>(mapped,page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PaymentDtos.View get(UUID id) { return views(List.of(payments.findById(id).orElseThrow(()->DomainException.missing("Payment")))).getFirst(); }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public PaymentDtos.View create(PaymentDtos.Request r) {
        LocalDate today=dates.today();refs.client(r.clientId());
        if(r.paymentDate().isAfter(today)) throw DomainException.invalid("Payment date cannot be in the future");
        Invoice invoice=r.invoiceId()==null?null:invoices.lockForAllocation(r.invoiceId(),true);
        if(invoice!=null && !invoice.getClientId().equals(r.clientId())) throw DomainException.invalid("Invoice does not belong to this client");
        Payment p=new Payment();p.setClientId(r.clientId());p.setInvoiceId(r.invoiceId());p.setPaymentDate(r.paymentDate());p.setNotes(r.notes());p.setStatus("RECORDED");
        payments.saveAndFlush(p);
        Currency reporting=Currency.valueOf(jdbc.queryForObject("select reporting_currency from organization_settings where id=1",String.class));
        for(var input:r.parts()) {
            PaymentPart part=new PaymentPart();part.setPaymentId(p.getId());part.setMethod(input.method());part.setCurrency(input.currency());
            part.setAmount(Money.amount(input.amount(),input.currency()));part.setBankName(input.bankName());part.setReferenceNumber(input.referenceNumber());
            part.setTransactionDate(input.transactionDate()==null?r.paymentDate():input.transactionDate());part.setNotes(input.notes());
            if(part.getTransactionDate().isAfter(today)) throw DomainException.invalid("Transaction date cannot be in the future");
            if(input.method().equals("BANK_TRANSFER") && (input.bankName()==null || input.bankName().isBlank())) throw DomainException.invalid("Bank transfer requires a bank name");
            if(input.method().equals("CHECK") != (input.check()!=null)) throw DomainException.invalid("CHECK requires exactly one physical check record");
            validateAttachment(input.attachmentFileId(),r.clientId());part.setAttachmentFileId(input.attachmentFileId());
            if(invoice!=null) {
                BigDecimal rate=input.currency()==invoice.getCurrency()?BigDecimal.ONE:input.allocationExchangeRate();
                if(rate==null) throw DomainException.invalid("Cross-currency invoice allocation requires an exchange-rate snapshot");
                if(input.currency()==invoice.getCurrency() && input.allocationExchangeRate()!=null && input.allocationExchangeRate().compareTo(BigDecimal.ONE)!=0)
                    throw DomainException.invalid("Same-currency allocation rate must equal one");
                part.setAllocationExchangeRate(rate);part.setAllocationAmount(Money.amount(Money.rounded(part.getAmount().multiply(rate),invoice.getCurrency()),invoice.getCurrency()));
                if(part.getAllocationAmount().signum()<=0) throw DomainException.invalid("Converted allocation rounds to zero");
            } else if(input.allocationExchangeRate()!=null) throw DomainException.invalid("Allocation rate requires an invoice");
            BigDecimal reportingRate=input.currency()==reporting?BigDecimal.ONE:input.exchangeRateToReportingCurrency();
            if(input.currency()==reporting && input.exchangeRateToReportingCurrency()!=null && input.exchangeRateToReportingCurrency().compareTo(BigDecimal.ONE)!=0)
                throw DomainException.invalid("Same-currency reporting rate must equal one");
            if(reportingRate!=null) {
                part.setReportingCurrency(reporting);part.setExchangeRateToReportingCurrency(reportingRate);
                part.setReportingAmount(Money.amount(Money.rounded(part.getAmount().multiply(reportingRate),reporting),reporting));
                if(part.getReportingAmount().signum()<=0) throw DomainException.invalid("Reporting amount rounds to zero");
            }
            parts.saveAndFlush(part);
            if(input.check()!=null) {
                var c=input.check();
                if(c.receivedDate().isAfter(today) || c.dueDate().isBefore(c.receivedDate())) throw DomainException.invalid("Invalid check received/due dates");
                CheckPayment check=new CheckPayment();check.setPaymentPartId(part.getId());check.setCheckNumber(c.checkNumber());check.setBankName(c.bankName());
                check.setIssuerName(c.issuerName());check.setAmount(part.getAmount());check.setCurrency(part.getCurrency());check.setReceivedDate(c.receivedDate());
                check.setDueDate(c.dueDate());check.setStatus("PENDING");check.setNotes(c.notes());validateAttachment(c.attachmentFileId(),r.clientId());check.setAttachmentFileId(c.attachmentFileId());
                checks.saveAndFlush(check);
                activity.record("checks",check.getId(),r.clientId(),"CREATED","Received check "+c.checkNumber()+"; pending collection");
            }
            activity.record("payments",p.getId(),r.clientId(),"PART_CREATED","Recorded "+part.getMethod()+" "+part.getAmount()+" "+part.getCurrency());
        }
        updateStatus(p);
        if(invoice!=null) invoices.recalculate(invoice);
        activity.record("payments",p.getId(),r.clientId(),"CREATED","Recorded compound payment with "+r.parts().size()+" parts");
        return get(p.getId());
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public PaymentDtos.View reverse(UUID id,String reason) {
        UUID invoiceId=payments.findById(id).orElseThrow(()->DomainException.missing("Payment")).getInvoiceId();
        Invoice invoice=invoiceId==null?null:invoices.lockForAllocation(invoiceId,true);
        var p=payments.lockById(id).orElseThrow(()->DomainException.missing("Payment"));
        if(p.getStatus().equals("REVERSED")) throw DomainException.conflict("Payment already reversed");
        p.setStatus("REVERSED");p.setReversedAt(clock.instant());p.setReversalReason(reason);payments.flush();
        if(invoice!=null) invoices.recalculate(invoice);
        activity.record("payments",id,p.getClientId(),"REVERSED","Reversed payment: "+reason);
        return get(id);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public PaymentDtos.CheckView transition(UUID id,PaymentDtos.Transition r) {
        var context=context(id);Invoice invoice=context.invoiceId()==null?null:invoices.lockForAllocation(context.invoiceId(),true);
        var payment=payments.lockById(context.paymentId()).orElseThrow(()->DomainException.missing("Payment"));
        var check=checks.lockById(id).orElseThrow(()->DomainException.missing("Check"));
        if(payment.getStatus().equals("REVERSED")) throw DomainException.conflict("Payment reversed");
        if(r.version()!=check.getVersion()) throw DomainException.conflict("Check changed; refresh before updating");
        CheckPolicy.transition(check.getStatus(),r.status());
        if(r.date().isAfter(dates.today()) || r.date().isBefore(check.getReceivedDate()) ||
            check.getDepositedDate()!=null && r.date().isBefore(check.getDepositedDate())) throw DomainException.invalid("Invalid check transition date");
        if(Set.of("BOUNCED","CANCELLED").contains(r.status()) && (r.reason()==null || r.reason().isBlank())) throw DomainException.invalid("A reason is required");
        String previous=check.getStatus();check.setStatus(r.status());
        switch(r.status()) {
            case "DEPOSITED" -> check.setDepositedDate(r.date());
            case "CLEARED" -> check.setClearedDate(r.date());
            case "BOUNCED" -> check.setBouncedDate(r.date());
            case "CANCELLED" -> check.setReversalReason(r.reason());
            default -> throw DomainException.invalid("Invalid check status");
        }
        checks.flush();updateStatus(payment);
        if(invoice!=null) invoices.recalculate(invoice);
        activity.record("checks",id,context.clientId(),"STATUS_CHANGED","Check "+check.getCheckNumber()+": "+previous+" → "+r.status()+(r.reason()==null?"":": "+r.reason()));
        return checkView(check);
    }
    @Transactional @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public PaymentDtos.CheckView reverseCheck(UUID id,String reason) {
        var context=context(id);Invoice invoice=context.invoiceId()==null?null:invoices.lockForAllocation(context.invoiceId(),true);
        var p=payments.lockById(context.paymentId()).orElseThrow(()->DomainException.missing("Payment"));
        var c=checks.lockById(id).orElseThrow(()->DomainException.missing("Check"));
        if(!"CLEARED".equals(c.getStatus()) || p.getStatus().equals("REVERSED")) throw DomainException.conflict("Only a collected check can be reversed");
        c.setReversedAt(clock.instant());c.setReversalReason(reason);c.setStatus("CANCELLED");checks.flush();updateStatus(p);
        if(invoice!=null) invoices.recalculate(invoice);
        activity.record("checks",id,context.clientId(),"REVERSED","Reversed cleared check "+c.getCheckNumber()+": "+reason);
        return checkView(c);
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public PageResponse<Map<String,Object>> checkList(CheckFilter f) {
        var pageable=f.pageable(Set.of("createdAt","dueDate","checkNumber","status","amount"));
        String sort=switch(f.getSort()) {case "dueDate"->"c.due_date";case "checkNumber"->"c.check_number";case "status"->"c.status";case "amount"->"c.amount";default->"c.created_at";};
        String where=" where (c.check_number like ? escape '=' or cl.display_name like ? escape '=' or i.invoice_number like ? escape '=')";
        List<Object> args=new ArrayList<>();String search="%"+ListFilter.escape(f.getQ())+"%";args.add(search);args.add(search);args.add(search);
        if(f.getClientId()!=null){where+=" and p.client_id=?";args.add(f.getClientId());}
        if(f.getProjectId()!=null){where+=" and i.project_id=?";args.add(f.getProjectId());}
        if(f.getCurrency()!=null){where+=" and c.currency=?";args.add(f.getCurrency().name());}
        if(f.getBankName()!=null && !f.getBankName().isBlank()) {
            if(f.getBankName().length()>255) throw DomainException.invalid("Bank filter is too long");
            where+=" and c.bank_name=?";args.add(f.getBankName());
        }
        if(f.getStatus()!=null && !f.getStatus().isBlank()) {
            if(Set.of("OPEN","PENDING_COLLECTION").contains(f.getStatus())) where+=" and c.status in ('PENDING','DEPOSITED') and p.status<>'REVERSED'";
            else {
                if(!Set.of("PENDING","DEPOSITED","CLEARED","BOUNCED","CANCELLED").contains(f.getStatus())) throw DomainException.invalid("Unsupported check status");
                where+=" and c.status=?";args.add(f.getStatus());
            }
        }
        if(f.getFrom()!=null){where+=" and c.due_date>=?";args.add(f.getFrom());}
        if(f.getTo()!=null){where+=" and c.due_date<=?";args.add(f.getTo());}
        String join=" from check_payments c join payment_parts pp on pp.id=c.payment_part_id join payments p on p.id=pp.payment_id"+
            " join clients cl on cl.id=p.client_id left join invoices i on i.id=p.invoice_id left join projects pr on pr.id=i.project_id";
        long count=jdbc.queryForObject("select count(*)"+join+where,Long.class,args.toArray());
        args.add(f.getSize());args.add(pageable.getOffset());
        args.add(0,dates.today());
        var data=jdbc.queryForList("select c.*,p.client_id,p.invoice_id,p.id payment_id,p.status payment_status,cl.display_name client_name,"+
            "i.invoice_number,i.project_id,pr.name project_name,i.currency invoice_currency,i.balance_due invoice_outstanding,"+
            "i.pending_check_amount invoice_pending_check_coverage,greatest(i.balance_due-i.pending_check_amount,0) invoice_uncovered_outstanding,"+
            "pp.allocation_amount,datediff(c.due_date,?) days_remaining"+join+where+" order by "+sort+" "+f.getDirection()+",c.id limit ? offset ?",args.toArray());
        return new PageResponse<>(data,f.getPage(),f.getSize(),count,(int)Math.ceil((double)count/f.getSize()));
    }
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public Map<String,Object> getCheck(UUID id) {
        context(id);
        Map<String,Object> result=new LinkedHashMap<>(jdbc.queryForMap("select c.*,p.client_id,p.invoice_id,p.id payment_id,p.status payment_status,"+
            "cl.display_name client_name,i.invoice_number,i.project_id,pr.name project_name,i.currency invoice_currency,i.balance_due invoice_outstanding,"+
            "i.pending_check_amount invoice_pending_check_coverage,greatest(i.balance_due-i.pending_check_amount,0) invoice_uncovered_outstanding,pp.allocation_amount"+
            " from check_payments c join payment_parts pp on pp.id=c.payment_part_id join payments p on p.id=pp.payment_id"+
            " join clients cl on cl.id=p.client_id left join invoices i on i.id=p.invoice_id left join projects pr on pr.id=i.project_id where c.id=?",id));
        result.put("days_remaining",java.time.temporal.ChronoUnit.DAYS.between(dates.today(),checks.findById(id).orElseThrow(()->DomainException.missing("Check")).getDueDate()));
        return result;
    }
    private record Context(UUID paymentId,UUID invoiceId,UUID clientId) {}
    private Context context(UUID id) {
        return jdbc.query("select p.id,p.invoice_id,p.client_id from check_payments c join payment_parts pp on pp.id=c.payment_part_id join payments p on p.id=pp.payment_id where c.id=?",
            rs->{if(!rs.next()) throw DomainException.missing("Check");return new Context(com.codevo.businessos.common.SqlIds.uuid(rs,1),com.codevo.businessos.common.SqlIds.uuid(rs,2),com.codevo.businessos.common.SqlIds.uuid(rs,3));},id);
    }
    private void updateStatus(Payment p) {
        var counts=jdbc.queryForMap("select count(*) total,sum(case when collectible then 1 else 0 end) collected from ledger_parts where payment_id=?",p.getId());
        long total=((Number)counts.get("total")).longValue(),collected=((Number)counts.get("collected")).longValue();
        p.setStatus(collected==total?"SETTLED":collected>0?"PARTIALLY_SETTLED":"RECORDED");payments.flush();
    }
    private void validateAttachment(UUID id,UUID clientId) {
        if(id==null) return;
        if(!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from file_metadata where id=? and client_id=?)",Boolean.class,id,clientId))) throw DomainException.invalid("Attachment does not belong to this client");
    }
    private List<PaymentDtos.View> views(List<Payment> records) {
        if(records.isEmpty()) return List.of();
        var allParts=parts.findByPaymentIdIn(records.stream().map(Payment::getId).toList());
        var allChecks=allParts.isEmpty()?List.<CheckPayment>of():checks.findByPaymentPartIdIn(allParts.stream().map(PaymentPart::getId).toList());
        var checkMap=allChecks.stream().collect(Collectors.toMap(CheckPayment::getPaymentPartId,c->c));
        var partMap=allParts.stream().collect(Collectors.groupingBy(PaymentPart::getPaymentId));
        return records.stream().map(p->new PaymentDtos.View(p.getId(),p.getClientId(),p.getInvoiceId(),p.getPaymentDate(),p.getStatus(),p.getNotes(),
            partMap.getOrDefault(p.getId(),List.of()).stream().map(pp->new PaymentDtos.PartView(pp.getId(),pp.getMethod(),pp.getAmount(),pp.getCurrency(),pp.getBankName(),pp.getReferenceNumber(),
                pp.getTransactionDate(),pp.getAllocationExchangeRate(),pp.getAllocationAmount(),pp.getExchangeRateToReportingCurrency(),pp.getReportingCurrency(),pp.getReportingAmount(),pp.getNotes(),pp.getAttachmentFileId(),
                checkMap.containsKey(pp.getId())?checkView(checkMap.get(pp.getId())):null)).toList(),p.getCreatedAt(),p.getReversedAt(),p.getReversalReason(),p.getVersion())).toList();
    }
    private PaymentDtos.CheckView checkView(CheckPayment c) {
        return new PaymentDtos.CheckView(c.getId(),c.getPaymentPartId(),c.getCheckNumber(),c.getAmount(),c.getCurrency(),c.getBankName(),c.getIssuerName(),c.getReceivedDate(),c.getDueDate(),
            c.getDepositedDate(),c.getClearedDate(),c.getBouncedDate(),c.getStatus(),c.getNotes(),c.getAttachmentFileId(),c.getReversedAt(),c.getReversalReason(),c.getVersion());
    }
}
