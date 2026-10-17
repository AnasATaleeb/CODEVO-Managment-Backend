package com.codevo.businessos.notification;

import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;

@Service
public class AttentionService {
    public record Item(UUID id,String type,String title,String message,String entityType,String route,int priority,LocalDate date) {}
    private final NamedParameterJdbcTemplate jdbc;private final Clock clock;
    public AttentionService(NamedParameterJdbcTemplate jdbc,Clock clock) { this.jdbc=jdbc;this.clock=clock; }
    public List<Item> attention(boolean finance) {
        var settings=jdbc.queryForMap("select * from organization_settings where id=1",Map.of());
        LocalDate today=clock.instant().atZone(ZoneId.of((String)settings.get("timezone"))).toLocalDate();
        var p=new MapSqlParameterSource().addValue("today",today)
            .addValue("payment",today.plusDays(((Number)settings.get("payment_warning_days")).intValue()))
            .addValue("renewal",today.plusDays(((Number)settings.get("subscription_warning_days")).intValue()))
            .addValue("deadline",today.plusDays(((Number)settings.get("deadline_warning_days")).intValue()));
        List<String> selects=new ArrayList<>();
        if(finance) {
            selects.add("select id,case when balance_due<=pending_check_amount then 'PAYMENT_CHECK_COVERED' when due_date<:today then 'PAYMENT_OVERDUE' else 'PAYMENT_DUE_SOON' end type,concat((select ac.display_name from clients ac where ac.id=invoices.client_id),' — ',invoice_number) title,"+
                "case when balance_due<=pending_check_amount then concat(balance_due,' ',currency,' outstanding is covered by checks; collect the checks') else concat(greatest(balance_due-pending_check_amount,0),' ',currency,' needs a payment arrangement') end message,"+
                "'invoices' entity_type,concat('/app/finance/receivables?focus=',id) route,case when balance_due>pending_check_amount and due_date<:today then 1 when balance_due<=pending_check_amount then 5 else 4 end priority,due_date date from invoices where balance_due>0 and status not in ('DRAFT','VOID','CANCELLED') and due_date<=:payment");
            selects.add("select c.id,case when c.status='BOUNCED' then 'CHECK_BOUNCED' else 'CHECK_DUE' end,concat('Check ',c.check_number),concat('Check ',lower(c.status),' at ',c.bank_name),'checks',concat('/app/finance/checks?focus=',c.id),case when c.status='BOUNCED' or c.due_date<:today then 1 else 4 end,c.due_date from check_payments c join payment_parts pp on pp.id=c.payment_part_id join payments p on p.id=pp.payment_id left join invoices ci on ci.id=p.invoice_id where p.status<>'REVERSED' and (c.status='BOUNCED' and (p.invoice_id is null or ci.status not in ('DRAFT','VOID','CANCELLED') and ci.balance_due>ci.pending_check_amount) or c.status in ('PENDING','DEPOSITED') and c.due_date<=date_add(:today,interval 7 day))");
            selects.add("select id,case when status='EXPIRED' or current_period_end<:today then 'SUBSCRIPTION_EXPIRED' else 'SUBSCRIPTION_EXPIRING' end,name,'Subscription renewal requires attention','subscriptions',concat('/app/finance/renewals?focus=',id),case when status='EXPIRED' or current_period_end<:today then 3 when renewal_date<=date_add(:today,interval 7 day) then 5 else 6 end,renewal_date from subscriptions where status in ('ACTIVE','PAST_DUE','EXPIRED') and renewal_date<=:renewal");
            selects.add("select ps.id,'UNINVOICED_WORK',concat(p.name,' — ',ps.custom_name),concat(pc.uninvoiced_amount,' ',ps.currency,' of agreed service value remains uninvoiced'),'project_services',concat('/app/projects/',ps.project_id),6,ps.delivery_date from project_services ps join project_service_commercial pc on pc.id=ps.id join projects p on p.id=ps.project_id where ps.status<>'CANCELLED' and p.status<>'CANCELLED' and pc.uninvoiced_amount>0");
            selects.add("select i.id,'UNPAID_RENEWAL',i.invoice_number,concat(greatest(i.balance_due-i.pending_check_amount,0),' ',i.currency,' uncovered on a renewal invoice'),'invoices',concat('/app/finance/invoices/',i.id),case when i.due_date<:today and i.balance_due>i.pending_check_amount then 1 else 5 end,i.due_date from subscription_renewal_invoices r join invoices i on i.id=r.invoice_id where i.status not in ('DRAFT','VOID','CANCELLED') and i.balance_due>0");
        }
        selects.add("select id,'PROJECT_DEADLINE',name,'Project deadline','projects',concat('/app/projects/',id),case when deadline<:today then 2 else 6 end,deadline from projects where status not in ('COMPLETED','CANCELLED','DELIVERED') and deadline<=:deadline");
        selects.add("select id,'TASK_DEADLINE',title,'Task deadline','tasks',concat('/app/tasks?focus=',id),case when due_at<:today then 2 else 6 end,date(due_at) from tasks where status not in ('DONE','CANCELLED') and due_at<date_add(:deadline,interval 1 day)");
        selects.add("select id,'FOLLOW_UP_DUE',name,'Lead follow-up due','leads',concat('/app/leads?focus=',id),7,date(next_follow_up_at) from leads where stage not in ('WON','LOST') and next_follow_up_at<date_add(:today,interval 1 day)");
        selects.add("select id,'CLIENT_WAITING',name,'Waiting for client','projects',concat('/app/projects/',id),8,deadline from projects where status='WAITING_CLIENT'");
        return jdbc.query("select * from ("+String.join(" union all ",selects)+") attention order by priority,date is null,date,id limit 100",p,
            (rs,n)->new Item(com.codevo.businessos.common.SqlIds.uuid(rs,"id"),rs.getString("type"),rs.getString("title"),rs.getString("message"),rs.getString("entity_type"),rs.getString("route"),rs.getInt("priority"),rs.getDate("date")==null?null:rs.getDate("date").toLocalDate()));
    }
}
