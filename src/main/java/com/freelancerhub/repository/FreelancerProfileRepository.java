package com.freelancerhub.repository;

import com.freelancerhub.model.FreelancerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Integer> {
}
