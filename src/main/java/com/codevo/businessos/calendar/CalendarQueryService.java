package com.codevo.businessos.calendar;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import com.codevo.businessos.common.*;
@Service
public class CalendarQueryService {
    private final NamedParameterJdbcTemplate jdbc;private final BusinessDates dates;
    public CalendarQueryService(NamedParameterJdbcTemplate jdbc,BusinessDates dates) { this.jdbc=jdbc;this.dates=dates; }
    public List<Map<String,Object>> events(LocalDate from,LocalDate to,String type,UUID clientId,UUID projectId,UUID assignedUserId,boolean finance,boolean sales) {
        if(from==null || to==null || from.isAfter(to) || from.plusDays(93).isBefore(to)) throw DomainException.invalid("Calendar range must be at most 93 days");
        List<String> queries=new ArrayList<>();
        queries.add("select id,'PROJECT_DEADLINE' type,name title,deadline start_at,client_id,id project_id,assigned_user_id,concat('/app/projects/',id) route from projects where deadline>=:from and deadline<:to");
        queries.add("select id,'TASK_DEADLINE',title,due_at,client_id,project_id,assigned_user_id,concat('/app/tasks?focus=',id) from tasks where due_at>=:instantFrom and due_at<:instantTo and status not in ('DONE','CANCELLED')");
        queries.add("select id,type,title,start_at,client_id,project_id,assigned_user_id,concat('/app/calendar?focus=',id) from calendar_events where start_at>=:instantFrom and start_at<:instantTo");
        queries.add("select id,'ASSET_EXPIRY',name,coalesce(expiry_date,renewal_date),client_id,project_id,null,concat('/app/clients/',client_id,'?tab=assets') from assets where coalesce(expiry_date,renewal_date)>=:from and coalesce(expiry_date,renewal_date)<:to");
        if(sales) queries.add("select id,'FOLLOW_UP',name,next_follow_up_at,converted_client_id,null,assigned_user_id,concat('/app/leads?focus=',id) from leads where next_follow_up_at>=:instantFrom and next_follow_up_at<:instantTo and stage not in ('WON','LOST')");
        if(finance) {
            queries.add("select id,'INVOICE_DUE',invoice_number,due_date,client_id,project_id,null,concat('/app/finance/invoices/',id) from invoices where due_date>=:from and due_date<:to and status not in ('PAID','VOID','CANCELLED','DRAFT')");
            queries.add("select id,'SUBSCRIPTION_RENEWAL',name,renewal_date,client_id,project_id,null,concat('/app/finance/subscriptions?focus=',id) from subscriptions where renewal_date>=:from and renewal_date<:to and status not in ('CANCELLED','DRAFT')");
            queries.add("select c.id,'CHECK_DUE',concat('Check ',c.check_number),c.due_date,p.client_id,i.project_id,null,concat('/app/finance/checks?focus=',c.id) from check_payments c join payment_parts pp on pp.id=c.payment_part_id join payments p on p.id=pp.payment_id left join invoices i on i.id=p.invoice_id where c.due_date>=:from and c.due_date<:to and c.status in ('PENDING','DEPOSITED') and p.status<>'REVERSED'");
        }
        ZoneId zone=dates.zone();
        var p=new MapSqlParameterSource().addValue("instantFrom",from.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()).addValue("instantTo",to.plusDays(1).atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()).addValue("from",from).addValue("to",to.plusDays(1)).addValue("type",type)
            .addValue("client",SqlIds.text(clientId)).addValue("project",SqlIds.text(projectId)).addValue("assignee",SqlIds.text(assignedUserId));
        var rows=jdbc.queryForList("select * from ("+String.join(" union all ",queries)+") events where (:type is null or type=:type) and (:client is null or client_id=:client) and (:project is null or project_id=:project) and (:assignee is null or assigned_user_id=:assignee) order by start_at,id limit 1000",p);
        rows.forEach(row->{ Object value=row.get("start_at");boolean allDay=Set.of("PROJECT_DEADLINE","ASSET_EXPIRY","INVOICE_DUE","SUBSCRIPTION_RENEWAL","CHECK_DUE").contains(row.get("type"));row.put("all_day",allDay);if(allDay) { if(value instanceof LocalDateTime dt) row.put("start_at",dt.toLocalDate());else if(value instanceof java.sql.Timestamp t) row.put("start_at",t.toLocalDateTime().toLocalDate());return; } if(value instanceof java.sql.Timestamp t) row.put("start_at",t.toInstant());else if(value instanceof LocalDateTime dt) row.put("start_at",dt.toInstant(ZoneOffset.UTC)); });
        return rows;
    }
}
