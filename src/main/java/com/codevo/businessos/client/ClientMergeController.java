package com.codevo.businessos.client;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/clients")
public class ClientMergeController {
    private final ClientMergeService service;
    public ClientMergeController(ClientMergeService service) { this.service=service; }
    @GetMapping("/duplicates") public List<ClientMergeService.Duplicate> duplicates(
        @RequestParam(defaultValue="") @Size(max=255) String name,@RequestParam(defaultValue="") @Size(max=255) String email,
        @RequestParam(defaultValue="") @Size(max=255) String phone,@RequestParam(required=false) UUID excludeId) { return service.duplicates(name,email,phone,excludeId); }
    @GetMapping("/{id}/merge-preview") public ClientMergeService.Preview preview(@PathVariable UUID id,@RequestParam UUID targetId) { return service.preview(id,targetId); }
    @PostMapping("/{id}/merge") public ClientDtos.View merge(@PathVariable UUID id,@Valid @RequestBody ClientMergeService.Merge request) { return service.merge(id,request); }
}
