package com.codevo.businessos.activity;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.codevo.businessos.common.*;

@Service
public class ActivityService {
    private final JdbcTemplate jdbc;
    public ActivityService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void record(String entityType, UUID entityId, UUID clientId, String action, String summary) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID actor = auth==null || "anonymousUser".equals(auth.getName()) ? null : UUID.fromString(auth.getName());
        jdbc.update("insert into activity_log(id,entity_type,entity_id,client_id,actor_user_id,action,summary) values(?,?,?,?,?,?,?)",
            UUID.randomUUID(),entityType,entityId,clientId,actor,action,summary);
    }
    @Transactional(readOnly=true)
    public PageResponse<Map<String,Object>> list(UUID clientId, UUID entityId, int page, int size, boolean finance) {
        if(page<0 || size<1 || size>100) throw DomainException.invalid("Invalid pagination");
        String where=" where (? is null or client_id=?) and (? is null or entity_id=?)";
        if(!finance) where+=" and entity_type not in ('invoices','payments','checks','subscriptions','expenses','billing_plans')";
        Object[] args={clientId,clientId,entityId,entityId};
        long count=jdbc.queryForObject("select count(*) from activity_log"+where,Long.class,args);
        List<Object> params=new ArrayList<>(Arrays.asList(args)); params.add(size);params.add(page*size);
        return new PageResponse<>(jdbc.queryForList("select * from activity_log"+where+" order by created_at desc limit ? offset ?",params.toArray()),page,size,count,(int)Math.ceil((double)count/size));
    }
}
