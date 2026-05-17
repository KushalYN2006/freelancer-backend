package com.freelancerhub.controller;

import com.freelancerhub.model.Contract;
import com.freelancerhub.model.Bid;
import com.freelancerhub.model.Notification;
import com.freelancerhub.model.Payment;
import com.freelancerhub.model.Project;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.BidRepository;
import com.freelancerhub.repository.ContractRepository;
import com.freelancerhub.repository.PaymentRepository;
import com.freelancerhub.repository.ProjectRepository;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.service.NotificationService;
import com.freelancerhub.utils.ApiResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
    private BidRepository bidRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PaymentRepository paymentRepository;

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
            createMissingContracts(bidRepository.findByFreelancerUserIdAndStatus(id, Bid.BidStatus.accepted));
            List<Contract> contracts = contractRepository.findByFreelancerUserId(id);
            return ResponseEntity.ok(contracts.stream().map(this::contractSummary).toList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch contracts: " + e.getMessage()));
        }
    }

    // ── GET /api/contracts/client/{id} ────────────────────────────────────────
    @GetMapping("/client/{id}")
    public ResponseEntity<?> getClientContracts(@PathVariable Integer id) {
        try {
            createMissingContracts(bidRepository.findByProjectClientUserIdAndStatus(id, Bid.BidStatus.accepted));
            List<Contract> contracts = contractRepository.findByProjectClientUserId(id);
            return ResponseEntity.ok(contracts.stream().map(this::contractSummary).toList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch contracts: " + e.getMessage()));
        }
    }

    // ── PUT /api/contracts/{id}/complete ──────────────────────────────────────
    @PutMapping("/{id}/payment-complete")
    public ResponseEntity<?> completePayment(@PathVariable Integer id,
                                             @RequestBody(required = false) Map<String, Object> body) {
        try {
            Contract contract = contractRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            Integer freelancerId = body != null && body.get("freelancerId") != null
                    ? ((Number) body.get("freelancerId")).intValue()
                    : null;

            if (freelancerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "freelancerId is required"));
            }

            if (!contract.getFreelancer().getUserId().equals(freelancerId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only the assigned freelancer can mark payment complete"));
            }

            if (contract.getStatus() == Contract.ContractStatus.completed) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Payment cannot be changed after contract completion"));
            }

            Payment payment = paymentRepository.findFirstByContractContractIdOrderByPaymentDateDesc(id)
                    .orElseGet(Payment::new);
            payment.setContract(contract);
            payment.setAmount(contract.getProject().getBudget() != null
                    ? contract.getProject().getBudget()
                    : BigDecimal.ZERO);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus(Payment.PaymentStatus.paid);
            Payment saved = paymentRepository.save(payment);

            Project project = contract.getProject();
            notificationService.create(
                    project.getClient(),
                    contract.getFreelancer(),
                    Notification.NotificationType.payment_completed,
                    "Payment completed",
                    contract.getFreelancer().getName() + " marked payment complete for " + project.getTitle(),
                    "contracts.html"
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Payment marked as completed",
                    "paymentId", saved.getPaymentId(),
                    "paymentStatus", saved.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeContract(@PathVariable Integer id) {
        try {
            Contract contract = contractRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            Payment payment = paymentRepository.findFirstByContractContractIdOrderByPaymentDateDesc(id)
                    .orElseThrow(() -> new RuntimeException("Freelancer must mark payment complete before the contract can be completed"));
            if (payment.getStatus() != Payment.PaymentStatus.paid) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Freelancer must mark payment complete before the contract can be completed"));
            }

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

    private void createMissingContracts(List<Bid> acceptedBids) {
        for (Bid bid : acceptedBids) {
            Project project = bid.getProject();
            User freelancer = bid.getFreelancer();
            if (contractRepository.existsByProjectProjectIdAndFreelancerUserId(
                    project.getProjectId(), freelancer.getUserId())) {
                continue;
            }

            Contract contract = new Contract();
            contract.setProject(project);
            contract.setFreelancer(freelancer);
            contract.setStartDate(LocalDate.now());
            contract.setEndDate(LocalDate.now().plusMonths(1));
            contract.setStatus(Contract.ContractStatus.active);
            contractRepository.save(contract);
        }
    }

    private Map<String, Object> contractSummary(Contract contract) {
        Map<String, Object> response = new LinkedHashMap<>(ApiResponseMapper.contractSummary(contract));
        Payment payment = paymentRepository.findFirstByContractContractIdOrderByPaymentDateDesc(contract.getContractId())
                .orElse(null);

        response.put("paymentId", payment != null ? payment.getPaymentId() : null);
        response.put("paymentStatus", payment != null ? payment.getStatus() : Payment.PaymentStatus.pending);
        response.put("paymentDate", payment != null ? payment.getPaymentDate() : null);
        return response;
    }
}
