package com.freelancerhub.repository;

import com.freelancerhub.model.FreelancerProfile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Integer> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO Freelancer_Profile (freelancer_id, skills, experience, portfolio_link)
            VALUES (:freelancerId, :skills, :experience, :portfolioLink)
            ON DUPLICATE KEY UPDATE
                skills = VALUES(skills),
                experience = VALUES(experience),
                portfolio_link = VALUES(portfolio_link)
            """, nativeQuery = true)
    int upsertProfile(@Param("freelancerId") Integer freelancerId,
                      @Param("skills") String skills,
                      @Param("experience") String experience,
                      @Param("portfolioLink") String portfolioLink);
}
