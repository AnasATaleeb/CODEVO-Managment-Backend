package com.codevo.businessos.config;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.*;
/** Explicitly binds externally visible CHAR(36) UUIDs when using JDBC alongside JPA. */
public class UuidJdbcTemplate extends JdbcTemplate {
    public UuidJdbcTemplate(DataSource source) { super(source); }
    @Override protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
        if(args==null) return super.newArgPreparedStatementSetter(null);
        Object[] normalized=args.clone();
        for(int i=0;i<normalized.length;i++) if(normalized[i] instanceof UUID id) normalized[i]=id.toString();
        return super.newArgPreparedStatementSetter(normalized);
    }
}
