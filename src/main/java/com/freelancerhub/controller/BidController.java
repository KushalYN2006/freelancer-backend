package com.freelancerhub.controller;

import com.freelancerhub.model.Bid;
import com.freelancerhub.model.Contract;
import com.freelancerhub.model.Notification;
import com.freelancerhub.model.Project;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.BidRepository;
import com.freelancerhub.repository.ContractRepository;
import com.freelancerhub.repository.ProjectRepository;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.service.NotificationService;
import com.freelancerhub.utils.ApiResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * BidController.java
 * Handles all REST API endpoints for bids.
 *
 * Endpoints:
 *   POST   /api/bids                     → freelancer places a bid
 *   GET    /api/projects/{id}/bids        → client views all bids on their project
 *   GET    /api/bids/freelancer/{id}      → freelancer views their own bids
 *   PUT    /api/bids/{id}/status          → client accepts or rejects a bid
 */
@RestController
@CrossOrigin(origins = "*")
public class BidController {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private NotificationService notificationService;

    // ── POST /api/bids ────────────────────────────────────────────────────────
    /**
     * Freelancer places a new bid on a project.
     * Request body must include: projectId, freelancerId, bidAmount, proposal
     */
    @PostMapping("/api/bids")
    public ResponseEntity<?> placeBid(@RequestBody Map<String, Object> body) {
        try {
            Integer projectId    = body.get("projectId") != null ? ((Number) body.get("projectId")).intValue() : null;
            Integer freelancerId = body.get("freelancerId") != null ? ((Number) body.get("freelancerId")).intValue() : null;
            Double  bidAmount    = body.get("bidAmount") != null ? Double.valueOf(body.get("bidAmount").toString()) : null;
            String  proposal     = (String)  body.get("proposal");

            // Validate required fields
            if (projectId == null || freelancerId == null || bidAmount == null || proposal == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "projectId, freelancerId, bidAmount, and proposal are required"));
            }

            // Check project exists
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

            if (project.getStatus() != Project.Status.open) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This project is no longer open for bids"));
            }

            // Check freelancer (user) exists
            User freelancer = userRepository.findById(freelancerId)
                    .orElseThrow(() -> new RuntimeException("Freelancer not found with id: " + freelancerId));

            if (freelancer.getRole() != User.Role.freelancer) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only freelancers can place bids"));
            }

            if (bidAmount <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Bid amount must be greater than 0"));
            }

            if (proposal.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Proposal is required"));
            }

            // Prevent duplicate bids from the same freelancer on the same project
            if (bidRepository.existsByProjectProjectIdAndFreelancerUserId(projectId, freelancerId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You have already placed a bid on this project"));
            }

            // Build and save the bid
            Bid bid = new Bid();
            bid.setProject(project);
            bid.setFreelancer(freelancer);
            bid.setBidAmount(bidAmount);
            bid.setProposal(proposal);
            bid.setStatus(Bid.BidStatus.pending);   // always starts as pending

            Bid savedBid = bidRepository.save(bid); // INSERT INTO Bids ...

            notificationService.create(
                    project.getClient(),
                    freelancer,
                    Notification.NotificationType.bid_placed,
                    "New bid received",
                    freelancer.getName() + " placed a $" + bidAmount + " bid on " + project.getTitle(),
                    "my-projects.html"
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Bid placed successfully",
                    "bidId",   savedBid.getBidId(),
                    "status",  savedBid.getStatus()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/projects/{id}/bids ───────────────────────────────────────────
    /**
     * Client views all bids received on one of their projects.
     */
    @GetMapping("/api/projects/{id}/bids")
    public ResponseEntity<?> getBidsForProject(@PathVariable Integer id) {
        try {
            // Check the project exists first
            if (!projectRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            List<Bid> bids = bidRepository.findByProjectProjectId(id);
            return ResponseEntity.ok(bids.stream().map(ApiResponseMapper::bidSummary).toList());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch bids: " + e.getMessage()));
        }
    }

    // ── GET /api/bids/freelancer/{freelancerId} ───────────────────────────────
    /**
     * Freelancer views all bids they have placed (My Bids page).
     */
    @GetMapping("/api/bids/freelancer/{freelancerId}")
    public ResponseEntity<?> getFreelancerBids(@PathVariable Integer freelancerId) {
        try {
            List<Bid> bids = bidRepository.findByFreelancerUserId(freelancerId);
            return ResponseEntity.ok(bids.stream().map(ApiResponseMapper::bidSummary).toList());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch freelancer bids: " + e.getMessage()));
        }
    }

    // ── PUT /api/bids/{id}/status ─────────────────────────────────────────────
    /**
     * Client accepts or rejects a bid.
     * Request body: { "status": "accepted" } or { "status": "rejected" }
     *
     * When a bid is accepted:
     *   - That bid becomes "accepted"
     *   - All other bids on the same project are automatically "rejected"
     *   - The project status changes to "in_progress"
     */
    @PutMapping("/api/bids/{id}/status")
    public ResponseEntity<?> updateBidStatus(@PathVariable Integer id,
                                              @RequestBody Map<String, String> body) {
        try {
            String statusStr = body.get("status");

            if (statusStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "status field is required (accepted or rejected)"));
            }

            Bid.BidStatus newStatus;
            try {
                newStatus = Bid.BidStatus.valueOf(statusStr.toLowerCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Use: accepted or rejected"));
            }

            Bid bid = bidRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Bid not found with id: " + id));

            bid.setStatus(newStatus);
            bidRepository.save(bid);

            Project project = bid.getProject();
            String resultWord = newStatus == Bid.BidStatus.accepted ? "accepted" : "rejected";
            notificationService.create(
                    bid.getFreelancer(),
                    project.getClient(),
                    newStatus == Bid.BidStatus.accepted
                            ? Notification.NotificationType.bid_accepted
                            : Notification.NotificationType.bid_rejected,
                    "Bid " + resultWord,
                    "Your bid on " + project.getTitle() + " was " + resultWord,
                    newStatus == Bid.BidStatus.accepted ? "contracts.html" : "my-bids.html"
            );

            // If accepting this bid, reject all other bids on the same project
            if (newStatus == Bid.BidStatus.accepted) {
                Integer projectId = bid.getProject().getProjectId();

                List<Bid> otherBids = bidRepository.findByProjectProjectId(projectId);
                for (Bid otherBid : otherBids) {
                    // Skip the bid we just accepted
                    if (!otherBid.getBidId().equals(id)) {
                        otherBid.setStatus(Bid.BidStatus.rejected);
                        bidRepository.save(otherBid);
                        notificationService.create(
                                otherBid.getFreelancer(),
                                project.getClient(),
                                Notification.NotificationType.bid_rejected,
                                "Bid rejected",
                                "Your bid on " + project.getTitle() + " was rejected",
                                "my-bids.html"
                        );
                    }
                }

                // Move project status to in_progress
                project.setStatus(Project.Status.in_progress);
                projectRepository.save(project);

                if (!contractRepository.existsByProjectProjectIdAndFreelancerUserId(projectId, bid.getFreelancer().getUserId())) {
                    Contract contract = new Contract();
                    contract.setProject(project);
                    contract.setFreelancer(bid.getFreelancer());
                    contract.setStartDate(LocalDate.now());
                    contract.setEndDate(LocalDate.now().plusMonths(1));
                    contract.setStatus(Contract.ContractStatus.active);
                    contractRepository.save(contract);

                    notificationService.create(
                            project.getClient(),
                            bid.getFreelancer(),
                            Notification.NotificationType.contract_created,
                            "Contract created",
                            "A contract with " + bid.getFreelancer().getName() + " was created for " + project.getTitle(),
                            "contracts.html"
                    );
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Bid status updated to " + newStatus,
                    "bidId",   id
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
