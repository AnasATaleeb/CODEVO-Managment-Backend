package com.codevo.businessos.file;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.codevo.businessos.common.*;
@RestController @RequestMapping("/api/v1/files")
public class FileController {
    private final FileService service;
    public FileController(FileService service) { this.service=service; }
    @GetMapping public PageResponse<FileService.Metadata> list(@RequestParam(required=false) UUID clientId,@RequestParam(required=false) UUID projectId,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(clientId,projectId,page,size); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public FileService.Metadata upload(@RequestPart MultipartFile file,@RequestParam(required=false) UUID clientId,@RequestParam(required=false) UUID projectId,
        @RequestParam(required=false) UUID invoiceId,@RequestParam(required=false) UUID expenseId,@RequestParam(required=false) UUID taskId,@RequestParam(required=false) UUID paymentId,
        @RequestParam(defaultValue="DOCUMENT") String category) throws IOException { return service.upload(file,clientId,projectId,invoiceId,expenseId,taskId,paymentId,category); }
    @GetMapping("/{id}/download") public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        var d=service.download(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.metadata().contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.metadata().originalName(),StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store").body(d.resource());
    }
}
