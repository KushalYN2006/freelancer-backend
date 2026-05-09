package com.freelancerhub.service;

import com.freelancerhub.model.Project;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.ProjectRepository;
import com.freelancerhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ProjectService.java
 * Contains all the business logic for projects.
 * Controllers call this service — this service talks to the repository (database).
 *
 * Separation of concerns:
 *   Controller → handles HTTP request/response
 *   Service    → handles business rules and validation    ← you are here
 *   Repository → handles database SQL queries
 */
@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // ── CREATE ────────────────────────────────────────────────────────────────
    /**
     * Creates a new project posted by a client.
     * Validates the client exists and has the 'client' role before saving.
     *
     * @param clientId    the user_id of the client posting the project
     * @param title       project title
     * @param description detailed description of the work needed
     * @param budget      maximum budget the client is willing to pay
     * @param deadline    date by which the work must be completed
     * @return the saved Project object with its new project_id
     */
    public Project createProject(Integer clientId, String title,
                                  String description, Double budget,
                                  java.time.LocalDate deadline) {

        // Validate inputs
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Project title cannot be empty");
        }
        if (budget == null || budget <= 0) {
            throw new RuntimeException("Budget must be a positive number");
        }
        if (deadline == null) {
            throw new RuntimeException("Deadline is required");
        }

        // Make sure the client exists in the Users table
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));

        // Make sure the user actually has the 'client' role
        if (client.getRole() != User.Role.client) {
            throw new RuntimeException("Only clients can post projects");
        }

        // Build the project object and save it — INSERT INTO Projects ...
        Project project = new Project();
        project.setClient(client);
        project.setTitle(title.trim());
        project.setDescription(description != null ? description.trim() : "");
        project.setBudget(new java.math.BigDecimal(budget.toString()));
        project.setDeadline(deadline);
        project.setStatus(Project.Status.open);       // always starts as 'open'
        project.setCreatedAt(LocalDateTime.now());

        return projectRepository.save(project);       // saves to MySQL, returns with project_id set
    }

    // ── READ ALL OPEN ─────────────────────────────────────────────────────────
    /**
     * Returns all projects with status = 'open'.
     * Used on the Browse Projects page for freelancers.
     */
    public List<Project> getAllOpenProjects() {
        return projectRepository.findByStatus(Project.Status.open);
    }

    // ── READ BY CLIENT ────────────────────────────────────────────────────────
    /**
     * Returns all projects posted by a specific client.
     * Used on the client's "My Projects" dashboard page.
     *
     * @param clientId the user_id of the client
     */
    public List<Project> getProjectsByClient(Integer clientId) {
        if (!userRepository.existsById(clientId)) {
            throw new RuntimeException("Client not found with id: " + clientId);
        }
        return projectRepository.findByClientUserId(clientId);
    }

    // ── READ BY ID ────────────────────────────────────────────────────────────
    /**
     * Returns a single project by its ID.
     * Used when a freelancer clicks on a project to view its details.
     */
    public Project getProjectById(Integer projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────
    /**
     * Searches projects by keyword in the title or description.
     * Used on the Browse Projects page search bar.
     *
     * @param keyword the search term entered by the freelancer
     */
    public List<Project> searchProjects(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllOpenProjects();   // empty search → return all open projects
        }
        return projectRepository.searchProjects(keyword.trim());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    /**
     * Updates an existing project's details.
     * Only the client who owns the project should be able to do this
     * (ownership check should also happen in the controller via JWT).
     *
     * @param projectId the ID of the project to update
     * @param title     new title (null = keep existing)
     * @param description new description (null = keep existing)
     * @param budget    new budget (null = keep existing)
     * @param deadline  new deadline (null = keep existing)
     * @param status    new status (null = keep existing)
     */
    public Project updateProject(Integer projectId,
                                  String title,
                                  String description,
                                  Double budget,
                                  java.time.LocalDate deadline,
                                  Project.Status status) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        // Only update the fields that were actually provided (non-null)
        if (title != null && !title.isBlank()) {
            project.setTitle(title.trim());
        }
        if (description != null) {
            project.setDescription(description.trim());
        }
        if (budget != null && budget > 0) {
            project.setBudget(new java.math.BigDecimal(budget.toString()));
        }
        if (deadline != null) {
            project.setDeadline(deadline);
        }
        if (status != null) {
            project.setStatus(status);
        }

        return projectRepository.save(project);   // UPDATE Projects SET ... WHERE project_id = ?
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    /**
     * Deletes a project by its ID.
     * Only 'open' projects should be deletable — you cannot delete a project
     * that is already in_progress or completed.
     *
     * @param projectId the ID of the project to delete
     */
    public void deleteProject(Integer projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        // Business rule: cannot delete projects that are already underway
        if (project.getStatus() != Project.Status.open) {
            throw new RuntimeException(
                "Cannot delete a project that is " + project.getStatus() +
                ". Only 'open' projects can be deleted."
            );
        }

        projectRepository.deleteById(projectId);   // DELETE FROM Projects WHERE project_id = ?
    }
}