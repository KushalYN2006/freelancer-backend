package com.freelancerhub.controller;

import com.freelancerhub.model.Bid;
import com.freelancerhub.model.Contract;
import com.freelancerhub.model.Project;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.BidRepository;
import com.freelancerhub.repository.ContractRepository;
import com.freelancerhub.repository.ProjectRepository;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.utils.ApiResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ContractRepository contractRepository;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientDashboard(@PathVariable Integer clientId) {
        try {
            User client = userRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));

            if (client.getRole() != User.Role.client) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a client"));
            }

            List<Project> projects = projectRepository.findByClientUserId(clientId);
            List<Bid> bids = bidRepository.findByProjectClientUserId(clientId);
            List<Contract> contracts = contractRepository.findByProjectClientUserId(clientId);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalProjects", projects.size());
            stats.put("openProjects", projects.stream().filter(p -> p.getStatus() == Project.Status.open).count());
            stats.put("activeContracts", contracts.stream().filter(c -> c.getStatus() == Contract.ContractStatus.active).count());
            stats.put("bidsReceived", bids.size());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("user", ApiResponseMapper.userSummary(client));
            response.put("stats", stats);
            response.put("recentProjects", projects.stream()
                    .sorted(Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(5)
                    .map(ApiResponseMapper::projectSummary)
                    .toList());
            response.put("recentBids", bids.stream()
                    .sorted(Comparator.comparing(Bid::getBidId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(5)
                    .map(ApiResponseMapper::bidSummary)
                    .toList());
            response.put("activeContracts", contracts.stream()
                    .filter(c -> c.getStatus() == Contract.ContractStatus.active)
                    .map(ApiResponseMapper::contractSummary)
                    .toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<?> getFreelancerDashboard(@PathVariable Integer freelancerId) {
        try {
            User freelancer = userRepository.findById(freelancerId)
                    .orElseThrow(() -> new RuntimeException("Freelancer not found with id: " + freelancerId));

            if (freelancer.getRole() != User.Role.freelancer) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is not a freelancer"));
            }

            List<Project> openProjects = projectRepository.findByStatus(Project.Status.open);
            List<Bid> bids = bidRepository.findByFreelancerUserId(freelancerId);
            List<Contract> contracts = contractRepository.findByFreelancerUserId(freelancerId);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("availableProjects", openProjects.size());
            stats.put("totalBids", bids.size());
            stats.put("acceptedBids", bids.stream().filter(b -> b.getStatus() == Bid.BidStatus.accepted).count());
            stats.put("activeContracts", contracts.stream().filter(c -> c.getStatus() == Contract.ContractStatus.active).count());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("user", ApiResponseMapper.userSummary(freelancer));
            response.put("stats", stats);
            response.put("recentProjects", openProjects.stream()
                    .sorted(Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(5)
                    .map(ApiResponseMapper::projectSummary)
                    .toList());
            response.put("recentBids", bids.stream()
                    .sorted(Comparator.comparing(Bid::getBidId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(5)
                    .map(ApiResponseMapper::bidSummary)
                    .toList());
            response.put("activeContracts", contracts.stream()
                    .filter(c -> c.getStatus() == Contract.ContractStatus.active)
                    .map(ApiResponseMapper::contractSummary)
                    .toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
