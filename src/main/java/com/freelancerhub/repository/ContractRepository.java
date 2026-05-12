package com.freelancerhub.repository;

import com.freelancerhub.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Integer> {

    // Get all contracts for a freelancer
    List<Contract> findByFreelancerUserId(Integer freelancerId);

    // Get all contracts for a client (through project)
    List<Contract> findByProjectClientUserId(Integer clientId);

    // Get contracts by status for a freelancer
    List<Contract> findByFreelancerUserIdAndStatus(Integer freelancerId, Contract.ContractStatus status);

    boolean existsByProjectProjectIdAndFreelancerUserId(Integer projectId, Integer freelancerId);
}
