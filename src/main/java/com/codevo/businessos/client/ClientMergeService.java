package com.codevo.businessos.client;

import com.codevo.businessos.activity.ActivityService;
import com.codevo.businessos.common.*;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class ClientMergeService {
    private static final String MERGE_PERMISSION="hasAuthority('CRM_WRITE') and hasAuthority('FINANCE_WRITE')";
    // Identifiers are a fixed allowlist, never supplied by a request.
    private static final List<String> TABLES=List.of("projects","tasks","invoices","payments","subscriptions","expenses","calendar_events","assets","client_notes","file_metadata","activity_log","leads");
    public record Duplicate(UUID id,String displayName,String email,String phone,String match) {}
    public record Preview(UUID sourceId,String sourceName,long sourceVersion,UUID targetId,String targetName,
        long targetVersion,Map<String,Long> records,String fingerprint) {}
    public record Merge(@NotNull UUID targetId,@NotNull Long sourceVersion,@NotNull Long targetVersion,
        @NotBlank @Size(max=64) String fingerprint,@NotBlank @Size(min=3,max=1000) String reason,@AssertTrue boolean confirmed) {}
    private final ClientRepository clients;
    private final ClientMapper mapper;
    private final JdbcTemplate jdbc;
    private final ActivityService activity;
    private final Clock clock;
    public ClientMergeService(ClientRepository clients,ClientMapper mapper,JdbcTemplate jdbc,ActivityService activity,Clock clock) {
        this.clients=clients;this.mapper=mapper;this.jdbc=jdbc;this.activity=activity;this.clock=clock;
    }
    @PreAuthorize("hasAuthority('CRM_READ')")
    public List<Duplicate> duplicates(String name,String email,String phone,UUID excludeId) {
        String normalizedPhone=phone==null?"":phone.replaceAll("[^0-9]","");
        String normalizedEmail=email==null?"":email.strip().toLowerCase(Locale.ROOT);
        String normalizedName=name==null?"":name.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
        return jdbc.query("select id,display_name,email,phone,case when ?<>'' and lower(trim(email))=? then 'EMAIL' when ?<>'' and regexp_replace(phone,'[^0-9]','')=? then 'PHONE' else 'NAME' end match_reason from clients where merged_into_id is null and (? is null or id<>?) and ((?<>'' and lower(trim(email))=?) or (?<>'' and regexp_replace(phone,'[^0-9]','')=?) or (?<>'' and lower(regexp_replace(trim(display_name),'[[:space:]]+',' '))=?)) order by display_name,id limit 20",
            (rs,n)->new Duplicate(SqlIds.uuid(rs,1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),normalizedEmail,normalizedEmail,normalizedPhone,normalizedPhone,excludeId,excludeId,normalizedEmail,normalizedEmail,normalizedPhone,normalizedPhone,normalizedName,normalizedName);
    }
    @PreAuthorize(MERGE_PERMISSION)
    public Preview preview(UUID sourceId,UUID targetId) {
        var source=clients.findById(sourceId).orElseThrow(()->DomainException.missing("Source client"));
        var target=clients.findById(targetId).orElseThrow(()->DomainException.missing("Target client"));
        validate(source,target);
        return describe(source,target,records(sourceId,false));
    }
    @Transactional @PreAuthorize(MERGE_PERMISSION)
    public ClientDtos.View merge(UUID sourceId,Merge request) {
        if(sourceId.equals(request.targetId()))throw DomainException.invalid("Choose two different clients");
        if(!request.confirmed())throw DomainException.invalid("Confirm the merge preview before continuing");
        var locked=new TreeMap<UUID,Client>();
        for(var id:new TreeSet<>(List.of(sourceId,request.targetId())))locked.put(id,clients.lockById(id).orElseThrow(()->DomainException.missing("Client")));
        var source=locked.get(sourceId);var target=locked.get(request.targetId());validate(source,target);
        if(source.getVersion()!=request.sourceVersion()||target.getVersion()!=request.targetVersion())throw DomainException.conflict("Client changed; refresh the merge preview");
        var records=records(sourceId,true);
        if(!describe(source,target,records).fingerprint().equals(request.fingerprint()))throw DomainException.conflict("Related records changed; refresh the merge preview");
        UUID mergeId=UUID.randomUUID();UUID actor=UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        jdbc.update("insert into client_merges(id,source_client_id,target_client_id,reason,actor_user_id) values(?,?,?,?,?)",mergeId,sourceId,target.getId(),request.reason(),actor);
        for(String table:TABLES) {
            for(String row:records.get(table))jdbc.update("insert into client_merge_records(merge_id,entity_type,entity_id) values(?,?,?)",mergeId,table,row.split(":",2)[0]);
            String column=table.equals("leads")?"converted_client_id":"client_id";
            String audit=Set.of("file_metadata","activity_log").contains(table)?"":",version=version+1,updated_at=now(6)";
            jdbc.update("update "+table+" set "+column+"=?"+audit+" where "+column+"=?",target.getId(),sourceId);
        }
        source.setMergedIntoId(target.getId());source.setMergedAt(clock.instant());source.setStatus("INACTIVE");
        // Change the target version even if its chosen profile fields are retained.
        target.touchForMerge(clock.instant());clients.flush();
        activity.record("clients",sourceId,target.getId(),"MERGED","Merged "+source.getDisplayName()+" into "+target.getDisplayName()+": "+request.reason());
        activity.record("clients",target.getId(),target.getId(),"MERGE_RECEIVED","Received client records from "+source.getDisplayName()+"; original financial snapshots retained");
        return mapper.view(target);
    }
    private void validate(Client source,Client target) {
        if(source.getId().equals(target.getId()))throw DomainException.invalid("Choose two different clients");
        if(source.getMergedIntoId()!=null||target.getMergedIntoId()!=null)throw DomainException.conflict("Use the active client profile; this client was already merged");
    }
    private Map<String,List<String>> records(UUID id,boolean lock) {
        Map<String,List<String>> records=new LinkedHashMap<>();
        for(String table:TABLES) {
            String column=table.equals("leads")?"converted_client_id":"client_id";
            String version=Set.of("file_metadata","activity_log").contains(table)?"'0'":"version";
            records.put(table,jdbc.query("select id,"+version+" from "+table+" where "+column+"=? order by id"+(lock?" for update":""),(rs,n)->rs.getString(1)+":"+rs.getString(2),id));
        }
        return records;
    }
    private Preview describe(Client source,Client target,Map<String,List<String>> rows) {
        Map<String,Long> counts=new LinkedHashMap<>();rows.forEach((table,ids)->counts.put(table,(long)ids.size()));
        String material=source.getId()+":"+source.getVersion()+":"+target.getId()+":"+target.getVersion()+":"+rows;
        try {
            String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
            return new Preview(source.getId(),source.getDisplayName(),source.getVersion(),target.getId(),target.getDisplayName(),target.getVersion(),counts,fingerprint);
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable",e); }
    }
}
