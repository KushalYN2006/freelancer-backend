package com.freelancerhub.controller;

import com.freelancerhub.model.FreelancerProfile;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.FreelancerProfileRepository;
import com.freelancerhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            String skills = joinSkills(freelancerProfileRepository.findSkillsByFreelancerId(freelancerId));

            return ResponseEntity.ok(profileResponse(profile, freelancer, skills));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{freelancerId}")
    public ResponseEntity<?> saveProfile(@PathVariable Integer freelancerId,
                                         @RequestBody Map<String, Object> body) {
        try {
            User freelancer = getFreelancer(freelancerId);

            List<String> skills = readSkills(body);
            String experience = readText(body, "experience");
            String portfolioLink = readText(body, "portfolioLink", "portfolio_link");

            if (skills.isEmpty() || experience.isBlank() || portfolioLink.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Skills, experience, and portfolio link are required"));
            }

            freelancerProfileRepository.upsertProfile(freelancerId, experience, portfolioLink);
            freelancerProfileRepository.deleteSkillsByFreelancerId(freelancerId);
            for (String skill : skills) {
                freelancerProfileRepository.insertSkill(freelancerId, skill);
            }

            FreelancerProfile saved = freelancerProfileRepository.findById(freelancerId)
                    .orElseGet(() -> {
                        FreelancerProfile refreshed = emptyProfile(freelancer);
                        refreshed.setExperience(experience);
                        refreshed.setPortfolioLink(portfolioLink);
                        return refreshed;
                    });
            String savedSkills = joinSkills(freelancerProfileRepository.findSkillsByFreelancerId(freelancerId));
            return ResponseEntity.ok(profileResponse(saved, freelancer, savedSkills));
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

    private List<String> readSkills(Map<String, Object> body) {
        Object value = body.get("skills");
        Set<String> skills = new LinkedHashSet<>();

        if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                addSkill(skills, item);
            }
        } else {
            String rawSkills = value == null ? "" : value.toString();
            for (String skill : rawSkills.split("[,\\n]")) {
                addSkill(skills, skill);
            }
        }

        return new ArrayList<>(skills);
    }

    private void addSkill(Set<String> skills, Object value) {
        if (value == null) {
            return;
        }

        String skill = value.toString().trim();
        if (skill.isBlank()) {
            return;
        }

        if (skill.length() > 100) {
            throw new RuntimeException("Each skill must be 100 characters or fewer");
        }

        skills.add(skill);
    }

    private String joinSkills(List<String> skills) {
        return String.join(", ", skills);
    }

    private Map<String, Object> profileResponse(FreelancerProfile profile, User freelancer, String skills) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("freelancer_id", freelancer.getUserId());
        response.put("freelancerId", freelancer.getUserId());
        response.put("name", freelancer.getName());
        response.put("email", freelancer.getEmail());
        response.put("skills", skills);
        response.put("experience", profile.getExperience());
        response.put("portfolio_link", profile.getPortfolioLink());
        response.put("portfolioLink", profile.getPortfolioLink());
        return response;
    }
}
