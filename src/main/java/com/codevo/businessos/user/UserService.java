package com.codevo.businessos.user;

import java.util.*;
import java.time.Clock;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codevo.businessos.common.*;

@Service
public class UserService {
    public record Request(@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(max=255) String name,
        @NotBlank @Size(min=12,max=72) String password,@NotNull @Pattern(regexp="OWNER|ADMIN|STAFF|ACCOUNTANT") String role) {}
    public record View(UUID id,String email,String name,String role,boolean active) {}
    private final JdbcTemplate jdbc;private final PasswordEncoder passwords;private final Clock clock; private final com.codevo.businessos.activity.ActivityService activity;
    public UserService(JdbcTemplate jdbc,PasswordEncoder passwords,Clock clock,com.codevo.businessos.activity.ActivityService activity) { this.jdbc=jdbc;this.passwords=passwords;this.clock=clock;this.activity=activity; }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public List<View> list() { return jdbc.query("select id,email,name,role,active from app_users where active=true order by name limit 100", (rs,n)->new View(com.codevo.businessos.common.SqlIds.uuid(rs,1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5))); }
    @Transactional @PreAuthorize("hasAuthority('USERS_WRITE')")
    public View create(Request r) {
        if(r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw DomainException.invalid("Password must be at most 72 UTF-8 bytes");
        UUID id=UUID.randomUUID();var now=java.sql.Timestamp.from(clock.instant());
        jdbc.update("insert into app_users(id,email,name,password_hash,role,active,created_at,updated_at,version) values(?,?,?,?,?,true,?,?,0)",
            id,r.email().trim().toLowerCase(Locale.ROOT),r.name(),passwords.encode(r.password()),r.role(),now,now);
        activity.record("users",id,null,"CREATED","Created team member with role "+r.role());
        return new View(id,r.email().trim().toLowerCase(Locale.ROOT),r.name(),r.role(),true);
    }
    @Transactional @PreAuthorize("hasAuthority('USERS_WRITE')")
    public void disable(UUID id,String reason) {
        jdbc.queryForList("select id from app_users where role='OWNER' order by id for update");
        String role=jdbc.query("select role from app_users where id=? and active=true",rs->rs.next()?rs.getString(1):null,id);
        if(role==null) throw DomainException.missing("User");
        if(role.equals("OWNER") && jdbc.queryForObject("select count(*) from app_users where role='OWNER' and active=true",Long.class)<=1) throw DomainException.conflict("Cannot disable the last owner");
        jdbc.update("update app_users set active=false,updated_at=?,version=version+1 where id=?",java.sql.Timestamp.from(clock.instant()),id);
        activity.record("users",id,null,"DISABLED","Disabled team member: "+reason);
        jdbc.update("update auth_sessions set revoked_at=? where user_id=? and revoked_at is null",java.sql.Timestamp.from(clock.instant()),id);
    }
}
