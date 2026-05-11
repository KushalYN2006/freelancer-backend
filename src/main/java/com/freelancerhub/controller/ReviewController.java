package com.freelancerhub.controller;

import com.freelancerhub.model.Project;
import com.freelancerhub.model.Review;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.ProjectRepository;
import com.freelancerhub.repository.ReviewRepository;
import com.freelancerhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReviewController.java
 * Handles all REST API endpoints for reviews and ratings.
 *
 * Endpoints:
 *   POST /api/reviews              → post a new review
 *   GET  /api/reviews/{userId}     → get all reviews for a user
 *   GET  /api/reviews/rating/{userId} → get average rating for a user
 */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // ── POST /api/reviews ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> postReview(@RequestBody Map<String, Object> body) {
        try {
            Integer projectId   = (Integer) body.get("projectId");
            Integer reviewerId  = (Integer) body.get("reviewerId");
            Integer revieweeId  = (Integer) body.get("revieweeId");
            Integer rating      = (Integer) body.get("rating");
            String  comment     = (String)  body.get("comment");

            if (projectId == null || reviewerId == null || revieweeId == null || rating == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "projectId, reviewerId, revieweeId and rating are required"));
            }
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Rating must be between 1 and 5"));
            }

            Project project  = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            User reviewer    = userRepository.findById(reviewerId)
                    .orElseThrow(() -> new RuntimeException("Reviewer not found"));
            User reviewee    = userRepository.findById(revieweeId)
                    .orElseThrow(() -> new RuntimeException("Reviewee not found"));

            Review review = new Review();
            review.setProject(project);
            review.setReviewer(reviewer);
            review.setReviewee(reviewee);
            review.setRating(rating);
            review.setComment(comment);

            Review saved = reviewRepository.save(review);
            return ResponseEntity.ok(Map.of("message", "Review posted", "reviewId", saved.getReviewId()));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/reviews/{userId} ─────────────────────────────────────────────
    @GetMapping("/{userId}")
    public ResponseEntity<?> getReviewsForUser(@PathVariable Integer userId) {
        try {
            List<Review> reviews = reviewRepository.findByRevieweeUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch reviews: " + e.getMessage()));
        }
    }

    // ── GET /api/reviews/rating/{userId} ──────────────────────────────────────
    @GetMapping("/rating/{userId}")
    public ResponseEntity<?> getAverageRating(@PathVariable Integer userId) {
        try {
            Double avg = reviewRepository.getAverageRatingByUserId(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            result.put("totalReviews", reviewRepository.findByRevieweeUserId(userId).size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch rating: " + e.getMessage()));
        }
    }

    // ── GET /api/reviews/given/{userId} ───────────────────────────────────────
    @GetMapping("/given/{userId}")
    public ResponseEntity<?> getReviewsGiven(@PathVariable Integer userId) {
        try {
            List<Review> reviews = reviewRepository.findByReviewerUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch reviews: " + e.getMessage()));
        }
    }
}
