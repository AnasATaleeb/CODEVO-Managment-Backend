package com.codevo.businessos.project;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/projects")
public class ProjectProgressController {
    private final ProjectProgressService service;
    public ProjectProgressController(ProjectProgressService service) { this.service=service; }
    @GetMapping("/{id}/progress") public Map<String,Object> progress(@PathVariable UUID id) { return service.progress(id); }
}
