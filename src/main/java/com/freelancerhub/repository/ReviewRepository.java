package com.freelancerhub.repository;

import com.freelancerhub.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Get all reviews received by a specific user (their profile reviews)
    List<Review> findByRevieweeUserId(Integer revieweeId);

    // Get all reviews written by a specific user
    List<Review> findByReviewerUserId(Integer reviewerId);

    // Get average rating for a user
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.userId = :userId")
    Double getAverageRatingByUserId(@Param("userId") Integer userId);

    // Get reviews for a specific project
    List<Review> findByProjectProjectId(Integer projectId);
}
