package com.freelancerhub.controller;

import com.freelancerhub.model.Contract;
import com.freelancerhub.model.Notification;
import com.freelancerhub.model.Project;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.ContractRepository;
import com.freelancerhub.repository.ProjectRepository;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ContractController.java
 * Handles all REST API endpoints for contracts.
 *
 * Endpoints:
 *   POST /api/contracts                         → create a new contract
 *   GET  /api/contracts/freelancer/{id}         → get contracts for a freelancer
 *   GET  /api/contracts/client/{id}             → get contracts for a client
 *   PUT  /api/contracts/{id}/complete           → mark contract as completed
 */
@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
public class ContractController {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // ── POST /api/contracts ───────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createContract(@RequestBody Map<String, Object> body) {
        try {
            Integer projectId    = body.get("projectId") != null ? ((Number) body.get("projectId")).intValue() : null;
            Integer freelancerId = body.get("freelancerId") != null ? ((Number) body.get("freelancerId")).intValue() : null;
            String  startDate    = (String)  body.get("startDate");
            String  endDate      = (String)  body.get("endDate");

            if (projectId == null || freelancerId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "projectId and freelancerId are required"));
            }

            Project project     = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            User freelancer     = userRepository.findById(freelancerId)
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));

            if (freelancer.getRole() != User.Role.freelancer) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Contract must be assigned to a freelancer"));
            }

            if (contractRepository.existsByProjectProjectIdAndFreelancerUserId(projectId, freelancerId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Contract already exists for this accepted bid"));
            }

            Contract contract = new Contract();
            contract.setProject(project);
            contract.setFreelancer(freelancer);
            contract.setStartDate(startDate != null ? LocalDate.parse(startDate) : LocalDate.now());
            contract.setEndDate(endDate != null ? LocalDate.parse(endDate) : LocalDate.now().plusMonths(1));
            contract.setStatus(Contract.ContractStatus.active);

            Contract saved = contractRepository.save(contract);

            notificationService.create(
                    freelancer,
                    project.getClient(),
                    Notification.NotificationType.contract_created,
                    "Contract created",
                    "A contract was created for " + project.getTitle(),
                    "contracts.html"
            );
            notificationService.create(
                    project.getClient(),
                    freelancer,
                    Notification.NotificationType.contract_created,
                    "Contract created",
                    "A contract with " + freelancer.getName() + " was created for " + project.getTitle(),
                    "contracts.html"
            );

            return ResponseEntity.ok(Map.of("message", "Contract created", "contractId", saved.getContractId()));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/contracts/freelancer/{id} ────────────────────────────────────
    @GetMapping("/freelancer/{id}")
    public ResponseEntity<?> getFreelancerContracts(@PathVariable Integer id) {
        try {
            List<Contract> contracts = contractRepository.findByFreelancerUserId(id);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch contracts: " + e.getMessage()));
        }
    }

    // ── GET /api/contracts/client/{id} ────────────────────────────────────────
    @GetMapping("/client/{id}")
    public ResponseEntity<?> getClientContracts(@PathVariable Integer id) {
        try {
            List<Contract> contracts = contractRepository.findByProjectClientUserId(id);
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch contracts: " + e.getMessage()));
        }
    }

    // ── PUT /api/contracts/{id}/complete ──────────────────────────────────────
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeContract(@PathVariable Integer id) {
        try {
            Contract contract = contractRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            contract.setStatus(Contract.ContractStatus.completed);
            contractRepository.save(contract);

            // Also update the project status
            Project project = contract.getProject();
            project.setStatus(Project.Status.completed);
            projectRepository.save(project);

            notificationService.create(
                    contract.getFreelancer(),
                    project.getClient(),
                    Notification.NotificationType.contract_completed,
                    "Contract completed",
                    "The contract for " + project.getTitle() + " was marked completed",
                    "contracts.html"
            );

            return ResponseEntity.ok(Map.of("message", "Contract marked as completed"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
