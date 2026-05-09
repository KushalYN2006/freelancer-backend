package com.freelancerhub.controller;

import com.freelancerhub.model.Project;
import com.freelancerhub.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ProjectController.java
 * Handles HTTP requests for project endpoints.
 * Delegates all business logic to ProjectService.
 */
@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    @Autowired
    private ProjectService projectService;   // ← now uses service, not repository directly

    // GET /api/projects
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllOpenProjects();
    }

    // GET /api/projects/search?keyword=website
    @GetMapping("/search")
    public List<Project> searchProjects(@RequestParam String keyword) {
        return projectService.searchProjects(keyword);
    }

    // GET /api/projects/client/{clientId}
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientProjects(@PathVariable Integer clientId) {
        try {
            return ResponseEntity.ok(projectService.getProjectsByClient(clientId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(projectService.getProjectById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/projects
    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Map<String, Object> body) {
        try {
            Integer clientId    = (Integer) body.get("clientId");
            String  title       = (String)  body.get("title");
            String  description = (String)  body.get("description");
            Double  budget      = Double.valueOf(body.get("budget").toString());
            LocalDate deadline  = LocalDate.parse(body.get("deadline").toString());

            Project saved = projectService.createProject(clientId, title, description, budget, deadline);

            return ResponseEntity.ok(Map.of(
                    "message",   "Project created successfully",
                    "projectId", saved.getProjectId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/projects/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> body) {
        try {
            String      title       = (String) body.get("title");
            String      description = (String) body.get("description");
            Double      budget      = body.get("budget") != null
                                        ? Double.valueOf(body.get("budget").toString()) : null;
            LocalDate   deadline    = body.get("deadline") != null
                                        ? LocalDate.parse(body.get("deadline").toString()) : null;
            Project.Status status   = body.get("status") != null
                                        ? Project.Status.valueOf(body.get("status").toString()) : null;

            Project updated = projectService.updateProject(id, title, description, budget, deadline, status);
            return ResponseEntity.ok(updated);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/projects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Integer id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}