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
            INSERT INTO freelancer_profile (freelancer_id, experience, portfolio_link)
            VALUES (:freelancerId, :experience, :portfolioLink)
            ON DUPLICATE KEY UPDATE
                experience = :experience,
                portfolio_link = :portfolioLink
            """, nativeQuery = true)
    int upsertProfile(@Param("freelancerId") Integer freelancerId,
                      @Param("experience") String experience,
                      @Param("portfolioLink") String portfolioLink);

    @Query(value = """
            SELECT skill
            FROM freelancer_skills
            WHERE freelancer_id = :freelancerId
            """, nativeQuery = true)
    java.util.List<String> findSkillsByFreelancerId(@Param("freelancerId") Integer freelancerId);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM freelancer_skills
            WHERE freelancer_id = :freelancerId
            """, nativeQuery = true)
    int deleteSkillsByFreelancerId(@Param("freelancerId") Integer freelancerId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO freelancer_skills (freelancer_id, skill)
            VALUES (:freelancerId, :skill)
            """, nativeQuery = true)
    int insertSkill(@Param("freelancerId") Integer freelancerId,
                    @Param("skill") String skill);
}
