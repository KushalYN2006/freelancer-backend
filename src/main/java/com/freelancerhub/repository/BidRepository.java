package com.freelancerhub.repository;

import com.freelancerhub.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * BidRepository.java
 * Provides all database operations for the Bids table.
 * JpaRepository gives free: save(), findById(), findAll(), deleteById()
 */
public interface BidRepository extends JpaRepository<Bid, Integer> {

    // Get all bids for a specific project (client views these)
    // SQL equivalent: SELECT * FROM Bids WHERE project_id = ?
    List<Bid> findByProjectProjectId(Integer projectId);

    // Get all bids placed by a specific freelancer
    // SQL equivalent: SELECT * FROM Bids WHERE freelancer_id = ?
    List<Bid> findByFreelancerUserId(Integer freelancerId);

    // Get all bids received across projects owned by one client
    List<Bid> findByProjectClientUserId(Integer clientId);

    // Check if a freelancer already bid on a project (prevent duplicate bids)
    boolean existsByProjectProjectIdAndFreelancerUserId(Integer projectId, Integer freelancerId);

    // Count how many bids a project has received
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.project.projectId = :projectId")
    Long countBidsByProject(@Param("projectId") Integer projectId);
}
