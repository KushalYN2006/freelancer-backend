package com.freelancerhub.repository;

import com.freelancerhub.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    // Get all open projects (for freelancers to browse)
    List<Project> findByStatus(Project.Status status);

    // Get projects by client
    List<Project> findByClientUserId(Integer clientId);

    // Search by keyword in title or description
    @Query("SELECT p FROM Project p WHERE p.title LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Project> searchProjects(String keyword);
}