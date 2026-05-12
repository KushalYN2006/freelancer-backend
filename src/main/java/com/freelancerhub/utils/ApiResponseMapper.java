package com.freelancerhub.utils;

import com.freelancerhub.model.Bid;
import com.freelancerhub.model.Contract;
import com.freelancerhub.model.ConversationInvitation;
import com.freelancerhub.model.Message;
import com.freelancerhub.model.Project;
import com.freelancerhub.model.User;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponseMapper {

    private ApiResponseMapper() {
    }

    public static Map<String, Object> userSummary(User user) {
        if (user == null) {
            return null;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", user.getUserId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return response;
    }

    public static Map<String, Object> projectSummary(Project project) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("projectId", project.getProjectId());
        response.put("clientId", project.getClient().getUserId());
        response.put("clientName", project.getClient().getName());
        response.put("title", project.getTitle());
        response.put("description", project.getDescription());
        response.put("budget", project.getBudget());
        response.put("deadline", project.getDeadline());
        response.put("status", project.getStatus());
        response.put("createdAt", project.getCreatedAt());
        return response;
    }

    public static Map<String, Object> bidSummary(Bid bid) {
        Project project = bid.getProject();
        User freelancer = bid.getFreelancer();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bidId", bid.getBidId());
        response.put("projectId", project.getProjectId());
        response.put("projectTitle", project.getTitle());
        response.put("budget", project.getBudget());
        response.put("deadline", project.getDeadline());
        response.put("clientId", project.getClient().getUserId());
        response.put("clientName", project.getClient().getName());
        response.put("freelancerId", freelancer.getUserId());
        response.put("freelancerName", freelancer.getName());
        response.put("bidAmount", bid.getBidAmount());
        response.put("proposal", bid.getProposal());
        response.put("status", bid.getStatus());
        return response;
    }

    public static Map<String, Object> contractSummary(Contract contract) {
        Project project = contract.getProject();
        User freelancer = contract.getFreelancer();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("contractId", contract.getContractId());
        response.put("projectId", project.getProjectId());
        response.put("projectTitle", project.getTitle());
        response.put("budget", project.getBudget());
        response.put("deadline", project.getDeadline());
        response.put("clientId", project.getClient().getUserId());
        response.put("clientName", project.getClient().getName());
        response.put("freelancerId", freelancer.getUserId());
        response.put("freelancerName", freelancer.getName());
        response.put("startDate", contract.getStartDate());
        response.put("endDate", contract.getEndDate());
        response.put("status", contract.getStatus());
        return response;
    }

    public static Map<String, Object> messageSummary(Message message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("messageId", message.getMessageId());
        response.put("sender", userSummary(message.getSender()));
        response.put("receiver", userSummary(message.getReceiver()));
        response.put("message", message.getMessage());
        response.put("timestamp", message.getTimestamp());
        return response;
    }

    public static Map<String, Object> invitationSummary(ConversationInvitation invitation) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("invitationId", invitation.getInvitationId());
        response.put("sender", userSummary(invitation.getSender()));
        response.put("receiver", userSummary(invitation.getReceiver()));
        response.put("status", invitation.getStatus());
        response.put("createdAt", invitation.getCreatedAt());
        response.put("respondedAt", invitation.getRespondedAt());
        return response;
    }
}
