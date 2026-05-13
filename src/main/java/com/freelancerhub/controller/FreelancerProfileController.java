package com.freelancerhub.controller;

import com.freelancerhub.model.FreelancerProfile;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.FreelancerProfileRepository;
import com.freelancerhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/freelancer-profiles")
@CrossOrigin(origins = "*")
public class FreelancerProfileController {

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{freelancerId}")
    public ResponseEntity<?> getProfile(@PathVariable Integer freelancerId) {
        try {
            User freelancer = getFreelancer(freelancerId);
            FreelancerProfile profile = freelancerProfileRepository.findById(freelancerId)
                    .orElseGet(() -> emptyProfile(freelancer));

            return ResponseEntity.ok(profileResponse(profile, freelancer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{freelancerId}")
    public ResponseEntity<?> saveProfile(@PathVariable Integer freelancerId,
                                         @RequestBody Map<String, Object> body) {
        try {
            User freelancer = getFreelancer(freelancerId);
            FreelancerProfile profile = freelancerProfileRepository.findById(freelancerId)
                    .orElseGet(() -> emptyProfile(freelancer));

            String skills = readText(body, "skills");
            String experience = readText(body, "experience");
            String portfolioLink = readText(body, "portfolioLink", "portfolio_link");

            if (skills.isBlank() || experience.isBlank() || portfolioLink.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Skills, experience, and portfolio link are required"));
            }

            profile.setSkills(skills);
            profile.setExperience(experience);
            profile.setPortfolioLink(portfolioLink);

            FreelancerProfile saved = freelancerProfileRepository.save(profile);
            return ResponseEntity.ok(profileResponse(saved, freelancer));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User getFreelancer(Integer freelancerId) {
        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer not found with id: " + freelancerId));

        if (freelancer.getRole() != User.Role.freelancer) {
            throw new RuntimeException("Profile settings are only available for freelancers");
        }

        return freelancer;
    }

    private FreelancerProfile emptyProfile(User freelancer) {
        FreelancerProfile profile = new FreelancerProfile();
        profile.setFreelancerId(freelancer.getUserId());
        profile.setUser(freelancer);
        profile.setSkills("");
        profile.setExperience("");
        profile.setPortfolioLink("");
        return profile;
    }

    private String readText(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private Map<String, Object> profileResponse(FreelancerProfile profile, User freelancer) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("freelancer_id", freelancer.getUserId());
        response.put("freelancerId", freelancer.getUserId());
        response.put("name", freelancer.getName());
        response.put("email", freelancer.getEmail());
        response.put("skills", profile.getSkills());
        response.put("experience", profile.getExperience());
        response.put("portfolio_link", profile.getPortfolioLink());
        response.put("portfolioLink", profile.getPortfolioLink());
        return response;
    }
}
