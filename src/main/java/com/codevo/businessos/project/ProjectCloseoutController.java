package com.codevo.businessos.project;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/projects/{id}/closeout")
public class ProjectCloseoutController {
    private final ProjectCloseoutService service;
    public ProjectCloseoutController(ProjectCloseoutService service) { this.service=service; }
    @GetMapping public ProjectCloseoutService.Review review(@PathVariable UUID id) { return service.review(id); }
    @PostMapping public ProjectCloseoutService.Review complete(@PathVariable UUID id,@Valid @RequestBody ProjectCloseoutService.Complete request) { return service.complete(id,request); }
}
