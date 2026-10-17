package com.codevo.businessos.config;
import java.util.*;
import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component
@org.springframework.core.annotation.Order(0)
public class OwnerBootstrap implements ApplicationRunner {
    private final AppProperties props;private final JdbcTemplate jdbc;private final PasswordEncoder passwords;private final Clock clock;
    public OwnerBootstrap(AppProperties props,JdbcTemplate jdbc,PasswordEncoder passwords,Clock clock) { this.props=props;this.jdbc=jdbc;this.passwords=passwords;this.clock=clock; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(props.bootstrapOwnerEmail()==null || props.bootstrapOwnerEmail().isBlank()) return;
        String password=props.bootstrapOwnerPassword();
        if(password==null || password.length()<12 || password.length()>72 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Bootstrap owner password must contain 12–72 bytes");
        String email=props.bootstrapOwnerEmail().trim().toLowerCase(Locale.ROOT);
        if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalStateException("Invalid bootstrap owner email");
        jdbc.queryForList("select id from organization_settings where id=1 for update");
        if(jdbc.queryForObject("select count(*) from app_users",Long.class)>0) return;
        var now=java.sql.Timestamp.from(clock.instant());
        jdbc.update("insert into app_users(id,email,name,password_hash,role,active,created_at,updated_at,version) values(?,?,?,?,'OWNER',true,?,?,0)",
            UUID.randomUUID(),email,"CODEVO Owner",passwords.encode(password),now,now);
    }
}
