package com.codevo.businessos.notification;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import com.codevo.businessos.security.Permissions;
@Service
public class AttentionJobs {
    private final JdbcTemplate jdbc;private final AttentionService attention;private final Clock clock;
    public AttentionJobs(JdbcTemplate jdbc,AttentionService attention,Clock clock) { this.jdbc=jdbc;this.attention=attention;this.clock=clock; }
    @Scheduled(fixedDelayString="${app.jobs.delay-ms:60000}",initialDelayString="${app.jobs.initial-delay-ms:10000}")
    @Transactional
    public void run() {
        jdbc.queryForList("select id from organization_settings where id=1 for update");
        String zone=jdbc.queryForObject("select timezone from organization_settings where id=1",String.class);
        LocalDate today=clock.instant().atZone(ZoneId.of(zone)).toLocalDate();
        var overdue=jdbc.queryForList("select id,client_id,invoice_number from invoices where balance_due>0 and due_date<? and status in ('ISSUED','PARTIALLY_PAID') order by id limit 1000 for update",today);
        for(var i:overdue) {
            jdbc.update("update invoices set status='OVERDUE',updated_at=now(6),version=version+1 where id=?",i.get("id"));
            log("invoices",i.get("id"),i.get("client_id"),"OVERDUE","Invoice "+i.get("invoice_number")+" became overdue");
        }
        var expired=jdbc.queryForList("select id,client_id,name from subscriptions where date_add(current_period_end,interval grace_period_days day)<? and status in ('ACTIVE','PAST_DUE') order by id limit 1000 for update",today);
        for(var s:expired) {
            jdbc.update("update subscriptions set status='EXPIRED',updated_at=now(6),version=version+1 where id=?",s.get("id"));
            log("subscriptions",s.get("id"),s.get("client_id"),"EXPIRED","Subscription "+s.get("name")+" expired");
        }
        List<AttentionService.Item> all=attention.attention(true);
        List<String> keys=all.stream().map(this::key).toList();
        if(keys.isEmpty()) jdbc.update("update notifications set resolved_at=now(6) where resolved_at is null");
        else jdbc.update("update notifications set resolved_at=now(6) where resolved_at is null and dedup_key not in ("+String.join(",",Collections.nCopies(keys.size(),"?"))+")",keys.toArray());
        var users=jdbc.queryForList("select id,role from app_users where active=true limit 1000");
        for(var user:users) for(var item:all) {
            boolean finance=Set.of("invoices","checks","subscriptions").contains(item.entityType());
            var permissions=Permissions.forRole((String)user.get("role"));
            if(finance && !permissions.contains("FINANCE_READ") || item.entityType().equals("leads") && !permissions.contains("CRM_WRITE")) continue;
            jdbc.update("insert into notifications(id,user_id,type,title,message,entity_type,entity_id,route,priority,dedup_key) values(?,?,?,?,?,?,?,?,?,?) on duplicate key update resolved_at=null",
                UUID.randomUUID(),user.get("id"),item.type(),item.title(),item.message(),item.entityType(),item.id(),item.route(),item.priority(),key(item));
        }
    }
    private String key(AttentionService.Item i) { return i.type()+":"+i.id()+":"+i.date(); }
    private void log(String type,Object id,Object client,String action,String summary) {
        jdbc.update("insert into activity_log(id,entity_type,entity_id,client_id,action,summary) values(?,?,?,?,?,?)",UUID.randomUUID(),type,id,client,action,summary);
    }
}
