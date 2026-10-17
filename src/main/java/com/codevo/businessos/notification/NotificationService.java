package com.codevo.businessos.notification;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import com.codevo.businessos.common.*;
@Service
public class NotificationService {
    private final JdbcTemplate jdbc;
    public NotificationService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private UUID actor() { return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName()); }
    @PreAuthorize("isAuthenticated()")
    public PageResponse<Map<String,Object>> list(int page,int size,boolean unread) {
        if(page<0 || size<1 || size>100) throw DomainException.invalid("Invalid pagination");
        String where=" where user_id=? and resolved_at is null"+(unread?" and read_at is null":"");
        long count=jdbc.queryForObject("select count(*) from notifications"+where,Long.class,actor());
        return new PageResponse<>(jdbc.queryForList("select * from notifications"+where+" order by priority,created_at desc limit ? offset ?",actor(),size,page*size),page,size,count,(int)Math.ceil((double)count/size));
    }
    @Transactional @PreAuthorize("isAuthenticated()")
    public void read(UUID id) {
        if(jdbc.update("update notifications set read_at=coalesce(read_at,now()) where id=? and user_id=?",id,actor())!=1) throw DomainException.missing("Notification");
    }
    @Transactional @PreAuthorize("isAuthenticated()")
    public void readAll() { jdbc.update("update notifications set read_at=coalesce(read_at,now()) where user_id=?",actor()); }
}
