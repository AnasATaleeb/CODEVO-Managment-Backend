package com.codevo.businessos.project;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.common.ReferenceChecks;
@Service
public class ProjectProgressService {
    private final JdbcTemplate jdbc;private final ReferenceChecks refs;
    public ProjectProgressService(JdbcTemplate jdbc,ReferenceChecks refs) { this.jdbc=jdbc;this.refs=refs; }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public Map<String,Object> progress(UUID id) {
        refs.project(id,null);
        var counts=jdbc.queryForMap("select count(*) total,coalesce(sum(case when status='DONE' then 1 else 0 end),0) done from tasks where project_id=? and status<>'CANCELLED'",id);
        long total=((Number)counts.get("total")).longValue(),done=((Number)counts.get("done")).longValue();
        counts.put("percentage",total==0?0:Math.round(100.0*done/total));return counts;
    }
}
