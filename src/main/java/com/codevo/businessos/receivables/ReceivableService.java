package com.codevo.businessos.receivables;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ReceivableService {
    private final NamedParameterJdbcTemplate jdbc;
    private final BusinessDates dates;
    public ReceivableService(NamedParameterJdbcTemplate jdbc, BusinessDates dates) { this.jdbc=jdbc; this.dates=dates; }

    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ReceivableDtos.Page list(ReceivableFilter f) {
        var pageable=f.pageable(Set.of("dueDate","invoiceNumber","outstandingAmount","uncoveredOutstanding","createdAt"));
        LocalDate today=dates.today();
        var p=new MapSqlParameterSource().addValue("today",today).addValue("week",today.plusDays(7))
            .addValue("q","%"+ListFilter.escape(f.getQ())+"%").addValue("client",SqlIds.text(f.getClientId()))
            .addValue("project",SqlIds.text(f.getProjectId())).addValue("invoice",SqlIds.text(f.getInvoiceId())).addValue("currency",f.getCurrency()==null?null:f.getCurrency().name())
            .addValue("from",f.getFrom()).addValue("to",f.getTo()).addValue("limit",f.getSize()).addValue("offset",pageable.getOffset());
        String where=" where i.status not in ('DRAFT','VOID','CANCELLED') and (:client is null or i.client_id=:client)"+
            " and (:project is null or i.project_id=:project) and (:invoice is null or i.id=:invoice) and (:currency is null or i.currency=:currency)"+
            " and (:from is null or i.due_date>=:from) and (:to is null or i.due_date<=:to)"+
            " and (i.invoice_number like :q escape '=' or c.display_name like :q escape '=' or p.name like :q escape '=' or exists(select 1 from invoice_items si where si.invoice_id=i.id and si.description like :q escape '='))";
        String status=f.getStatus();
        where+=switch(status==null || status.isBlank()?"OPEN":status) {
            case "OPEN" -> " and i.balance_due>0";
            case "ALL" -> "";
            case "OVERDUE" -> " and i.balance_due>0 and i.due_date<:today";
            case "DUE_TODAY" -> " and i.balance_due>0 and i.due_date=:today";
            case "DUE_SOON" -> " and i.balance_due>0 and i.due_date>:today and i.due_date<=:week";
            case "COVERED" -> " and i.balance_due>0 and i.pending_check_amount>=i.balance_due";
            case "UNCOVERED" -> " and i.balance_due>i.pending_check_amount";
            case "PAID" -> " and i.balance_due=0";
            case "PARTIALLY_PAID" -> " and i.paid_amount>0 and i.balance_due>0 and i.due_date>=:today";
            case "ISSUED" -> " and i.paid_amount=0 and i.balance_due>0 and i.due_date>=:today";
            default -> throw DomainException.invalid("Unsupported receivable status");
        };
        String join=" from invoices i join clients c on c.id=i.client_id left join projects p on p.id=i.project_id";
        long count=jdbc.queryForObject("select count(*)"+join+where,p,Long.class);
        var totals=jdbc.query("select i.currency,sum(i.total) invoiced,sum(i.paid_amount) collected,sum(i.pending_check_amount) pending,"+
            "sum(i.balance_due) outstanding,sum(greatest(i.balance_due-i.pending_check_amount,0)) uncovered,"+
            "sum(case when i.due_date<:today then i.balance_due else 0 end) overdue,"+
            "sum(case when i.due_date=:today then i.balance_due else 0 end) due_today,"+
            "sum(case when i.due_date>:today and i.due_date<=:week then i.balance_due else 0 end) due_week"+
            join+where+" group by i.currency order by i.currency",p,(rs,n)->new ReceivableDtos.Totals(
                Currency.valueOf(rs.getString("currency")),rs.getBigDecimal("invoiced"),rs.getBigDecimal("collected"),rs.getBigDecimal("pending"),
                rs.getBigDecimal("outstanding"),rs.getBigDecimal("uncovered"),rs.getBigDecimal("overdue"),rs.getBigDecimal("due_today"),rs.getBigDecimal("due_week")));
        String sort=switch(f.getSort()) { case "invoiceNumber"->"i.invoice_number"; case "outstandingAmount"->"i.balance_due";
            case "uncoveredOutstanding"->"greatest(i.balance_due-i.pending_check_amount,0)";case "createdAt"->"i.created_at";default->"i.due_date"; };
        var rows=jdbc.query("select i.*,c.display_name client_name,p.name project_name,"+
            "(select group_concat(ii.description order by ii.id separator ', ') from invoice_items ii where ii.invoice_id=i.id) services,"+
            "exists(select 1 from ledger_parts l where l.invoice_id=i.id and l.bounced) has_bounced"+
            join+where+" order by "+sort+" "+f.getDirection()+",i.id limit :limit offset :offset",p,(rs,n)->{
                var outstanding=rs.getBigDecimal("balance_due");var pending=rs.getBigDecimal("pending_check_amount");
                var collected=rs.getBigDecimal("paid_amount");LocalDate due=rs.getDate("due_date").toLocalDate();
                String invoiceStatus=outstanding.signum()==0?"PAID":due.isBefore(today)?"OVERDUE":collected.signum()>0?"PARTIALLY_PAID":"ISSUED";
                var uncovered=outstanding.subtract(pending).max(java.math.BigDecimal.ZERO);
                String coverage=outstanding.signum()==0?"PAID":uncovered.signum()==0?"COVERED_BY_CHECKS":pending.signum()>0?"PARTIALLY_COVERED":"UNCOVERED";
                return new ReceivableDtos.Row(SqlIds.uuid(rs,"id"),rs.getString("invoice_number"),SqlIds.uuid(rs,"client_id"),rs.getString("client_name"),
                    SqlIds.uuid(rs,"project_id"),rs.getString("project_name"),rs.getString("services"),Currency.valueOf(rs.getString("currency")),
                    rs.getBigDecimal("total"),collected,pending,outstanding,uncovered,due,Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(due,today)),
                    invoiceStatus,coverage,outstanding.signum()==0?"VIEW_INVOICE":uncovered.signum()==0?"COLLECT_CHECKS":"RECORD_PAYMENT",rs.getBoolean("has_bounced"));
            });
        return new ReceivableDtos.Page(rows,f.getPage(),f.getSize(),count,(int)((count+f.getSize()-1)/f.getSize()),totals,today);
    }
}
