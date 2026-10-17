package com.codevo.businessos.file;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import com.codevo.businessos.common.*;
import com.codevo.businessos.activity.ActivityService;

@Service
public class FileService {
    public record Metadata(UUID id,UUID clientId,UUID projectId,UUID invoiceId,UUID expenseId,UUID taskId,UUID paymentId,String category,
        String originalName,String contentType,long size,java.time.Instant createdAt) {}
    public record Download(Metadata metadata,Resource resource) {}
    private final FileStorage storage;private final JdbcTemplate jdbc;private final ReferenceChecks refs;private final ActivityService activity;
    public FileService(FileStorage storage,JdbcTemplate jdbc,ReferenceChecks refs,ActivityService activity) { this.storage=storage;this.jdbc=jdbc;this.refs=refs;this.activity=activity; }
    @Transactional @PreAuthorize("hasAuthority('WORK_WRITE') or hasAuthority('FINANCE_WRITE')")
    public Metadata upload(MultipartFile file,UUID clientId,UUID projectId,UUID invoiceId,UUID expenseId,UUID taskId,UUID paymentId,String category) throws IOException {
        if(!Set.of("CONTRACT","PROPOSAL","INVOICE","RECEIPT","DESIGN","BRAND_ASSET","SCREENSHOT","DOCUMENT","APK","VIDEO","OTHER").contains(category)) throw DomainException.invalid("Invalid file category");
        if(clientId==null && projectId==null && invoiceId==null && expenseId==null && taskId==null && paymentId==null) throw DomainException.invalid("File needs a related business record");
        boolean financial=invoiceId!=null || expenseId!=null || paymentId!=null || Set.of("INVOICE","RECEIPT").contains(category);
        requirePermission(financial?"FINANCE_WRITE":"WORK_WRITE");
        if(clientId!=null) refs.client(clientId);
        if(projectId!=null) { refs.project(projectId,clientId);if(clientId==null) clientId=refs.projectClient(projectId); }
        for(var relation:List.of(new Relation("invoices",invoiceId),new Relation("expenses",expenseId),new Relation("tasks",taskId),new Relation("payments",paymentId))) {
            if(relation.id()==null) continue;
            UUID actual=jdbc.query("select client_id from "+relation.table()+" where id=?",rs->{if(!rs.next()) throw DomainException.missing("Related record");return com.codevo.businessos.common.SqlIds.uuid(rs,1);},relation.id());
            if(actual!=null && clientId!=null && !actual.equals(clientId)) throw DomainException.invalid("File relationships belong to different clients");
            if(actual!=null) clientId=actual;
            if(projectId!=null && relation.table().equals("payments")) {
                UUID parent=jdbc.query("select i.project_id from payments p left join invoices i on i.id=p.invoice_id where p.id=?",rs->rs.next()?SqlIds.uuid(rs,1):null,relation.id());
                if(parent!=null && !parent.equals(projectId)) throw DomainException.invalid("Payment attachment belongs to another project");
            }
            if(projectId!=null && !relation.table().equals("payments")) {
                UUID parent=jdbc.queryForObject("select project_id from "+relation.table()+" where id=?",UUID.class,relation.id());
                if(parent!=null && !parent.equals(projectId)) throw DomainException.invalid("File relationships belong to different projects");
            }
        }
        if(file.isEmpty() || file.getSize()>20*1024*1024) throw DomainException.invalid("File must be between 1 byte and 20 MB");
        byte[] bytes=file.getBytes();String mime=type(bytes);
        if(!mime.equals(file.getContentType())) throw DomainException.invalid("File MIME type does not match its signature");
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("attachment").replaceAll("[\\r\\n\\x00-\\x1F]","_");
        name=PathName.basename(name);if(name.length()>255) name=name.substring(name.length()-255);
        String key=storage.store(bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) try { storage.remove(key); } catch(IOException e) { org.slf4j.LoggerFactory.getLogger(FileService.class).warn("Unable to remove rolled-back file"); } }
        });
        UUID id=UUID.randomUUID(),actor=UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        jdbc.update("insert into file_metadata(id,client_id,project_id,invoice_id,expense_id,task_id,payment_id,category,original_name,storage_key,content_type,size,created_by) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id,clientId,projectId,invoiceId,expenseId,taskId,paymentId,category,name,key,mime,bytes.length,actor);
        activity.record("files",id,clientId,"UPLOADED","Uploaded "+name);return metadata(id);
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<Metadata> list(UUID clientId,UUID projectId,int page,int size) {
        if(page<0 || size<1 || size>100) throw DomainException.invalid("Invalid pagination");
        String where=" where (? is null or client_id=?) and (? is null or project_id=?)";
        if(!has("FINANCE_READ")) where+=" and invoice_id is null and expense_id is null and payment_id is null and category not in ('INVOICE','RECEIPT')";
        long count=jdbc.queryForObject("select count(*) from file_metadata"+where,Long.class,clientId,clientId,projectId,projectId);
        return new PageResponse<>(jdbc.query("select * from file_metadata"+where+" order by created_at desc limit ? offset ?",(rs,n)->map(rs),clientId,clientId,projectId,projectId,size,page*size),page,size,count,(int)Math.ceil((double)count/size));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public Download download(UUID id) throws IOException {
        var meta=metadata(id);
        if(meta.invoiceId()!=null || meta.expenseId()!=null || meta.paymentId()!=null || Set.of("INVOICE","RECEIPT").contains(meta.category())) requirePermission("FINANCE_READ");
        String key=jdbc.queryForObject("select storage_key from file_metadata where id=?",String.class,id);
        try { return new Download(meta,storage.read(key)); } catch(FileNotFoundException e) { throw DomainException.missing("Stored attachment"); }
    }
    private Metadata metadata(UUID id) { return jdbc.query("select * from file_metadata where id=?",rs->{if(!rs.next()) throw DomainException.missing("File");return map(rs);},id); }
    private Metadata map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Metadata(com.codevo.businessos.common.SqlIds.uuid(rs,"id"),com.codevo.businessos.common.SqlIds.uuid(rs,"client_id"),com.codevo.businessos.common.SqlIds.uuid(rs,"project_id"),com.codevo.businessos.common.SqlIds.uuid(rs,"invoice_id"),
            com.codevo.businessos.common.SqlIds.uuid(rs,"expense_id"),com.codevo.businessos.common.SqlIds.uuid(rs,"task_id"),com.codevo.businessos.common.SqlIds.uuid(rs,"payment_id"),rs.getString("category"),rs.getString("original_name"),rs.getString("content_type"),rs.getLong("size"),rs.getTimestamp("created_at").toInstant());
    }
    private record Relation(String table,UUID id) {}
    private boolean has(String p) { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals(p)); }
    private void requirePermission(String p) { if(!has(p)) throw new org.springframework.security.access.AccessDeniedException("Permission required"); }
    public static String type(byte[] b) {
        if(b.length>=5 && new String(b,0,5,StandardCharsets.US_ASCII).equals("%PDF-")) return "application/pdf";
        if(b.length>=8 && Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if(b.length>=3 && b[0]==(byte)255 && b[1]==(byte)216 && b[2]==(byte)255) return "image/jpeg";
        if(b.length>=12 && new String(b,0,4,StandardCharsets.US_ASCII).equals("RIFF") && new String(b,8,4,StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        if(b.length>=12 && new String(b,4,4,StandardCharsets.US_ASCII).equals("ftyp")) return "video/mp4";
        if(b.length>=4 && b[0]=='P' && b[1]=='K' && b[2]==3 && b[3]==4) { ApkSignature.validate(b);return "application/vnd.android.package-archive"; }
        throw DomainException.invalid("Supported uploads: PDF, PNG, JPEG, WebP, MP4 and structurally valid APK deliverables. Other executable and archive formats are excluded.");
    }
    private static class PathName { static String basename(String input) { return input.replace('\\','/').substring(input.replace('\\','/').lastIndexOf('/')+1); } }
}
