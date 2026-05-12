package com.freelancerhub.controller;

import com.freelancerhub.model.ConversationInvitation;
import com.freelancerhub.model.Notification;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.ConversationInvitationRepository;
import com.freelancerhub.repository.MessageRepository;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.service.NotificationService;
import com.freelancerhub.utils.ApiResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversation-invitations")
@CrossOrigin(origins = "*")
public class ConversationInvitationController {

    @Autowired
    private ConversationInvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<?> createInvitation(@RequestBody Map<String, Object> body) {
        try {
            Integer senderId = body.get("senderId") != null ? ((Number) body.get("senderId")).intValue() : null;
            Integer receiverId = body.get("receiverId") != null ? ((Number) body.get("receiverId")).intValue() : null;

            if (senderId == null || receiverId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "senderId and receiverId are required"));
            }
            if (senderId.equals(receiverId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You cannot invite yourself"));
            }

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));

            if (messageRepository.existsBySenderUserIdAndReceiverUserId(senderId, receiverId)
                    || messageRepository.existsBySenderUserIdAndReceiverUserId(receiverId, senderId)
                    || invitationRepository.existsBySenderUserIdAndReceiverUserIdAndStatus(
                            senderId, receiverId, ConversationInvitation.InvitationStatus.accepted)
                    || invitationRepository.existsBySenderUserIdAndReceiverUserIdAndStatus(
                            receiverId, senderId, ConversationInvitation.InvitationStatus.accepted)) {
                return ResponseEntity.ok(Map.of(
                        "message", "Conversation already available",
                        "status", "accepted"
                ));
            }

            var recent = invitationRepository
                    .findFirstBySenderUserIdAndReceiverUserIdOrderByCreatedAtDesc(senderId, receiverId);
            if (recent.isPresent() && recent.get().getStatus() == ConversationInvitation.InvitationStatus.pending) {
                return ResponseEntity.ok(ApiResponseMapper.invitationSummary(recent.get()));
            }

            ConversationInvitation invitation = new ConversationInvitation();
            invitation.setSender(sender);
            invitation.setReceiver(receiver);
            invitation.setStatus(ConversationInvitation.InvitationStatus.pending);
            ConversationInvitation saved = invitationRepository.save(invitation);

            notificationService.create(
                    receiver,
                    sender,
                    Notification.NotificationType.conversation_invite,
                    "Conversation request",
                    sender.getName() + " wants to start a conversation with you",
                    "notifications.html?invitationId=" + saved.getInvitationId()
            );

            return ResponseEntity.ok(ApiResponseMapper.invitationSummary(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending/{receiverId}")
    public ResponseEntity<?> pendingInvitations(@PathVariable Integer receiverId) {
        List<ConversationInvitation> invitations = invitationRepository.findByReceiverUserIdAndStatus(
                receiverId, ConversationInvitation.InvitationStatus.pending);
        return ResponseEntity.ok(invitations.stream().map(ApiResponseMapper::invitationSummary).toList());
    }

    @PutMapping("/{invitationId}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Integer invitationId) {
        return respond(invitationId, ConversationInvitation.InvitationStatus.accepted);
    }

    @PutMapping("/{invitationId}/reject")
    public ResponseEntity<?> rejectInvitation(@PathVariable Integer invitationId) {
        return respond(invitationId, ConversationInvitation.InvitationStatus.rejected);
    }

    private ResponseEntity<?> respond(Integer invitationId, ConversationInvitation.InvitationStatus status) {
        try {
            ConversationInvitation invitation = invitationRepository.findById(invitationId)
                    .orElseThrow(() -> new RuntimeException("Invitation not found"));

            if (invitation.getStatus() != ConversationInvitation.InvitationStatus.pending) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invitation already " + invitation.getStatus()));
            }

            invitation.setStatus(status);
            invitation.setRespondedAt(LocalDateTime.now());
            ConversationInvitation saved = invitationRepository.save(invitation);

            boolean accepted = status == ConversationInvitation.InvitationStatus.accepted;
            notificationService.create(
                    invitation.getSender(),
                    invitation.getReceiver(),
                    accepted
                            ? Notification.NotificationType.conversation_invite_accepted
                            : Notification.NotificationType.conversation_invite_rejected,
                    accepted ? "Conversation accepted" : "Conversation rejected",
                    invitation.getReceiver().getName() + (accepted
                            ? " accepted your conversation request"
                            : " rejected your conversation request"),
                    accepted
                            ? "messages.html?to=" + invitation.getReceiver().getUserId()
                                    + "&name=" + URLEncoder.encode(invitation.getReceiver().getName(), StandardCharsets.UTF_8)
                            : "notifications.html"
            );

            return ResponseEntity.ok(ApiResponseMapper.invitationSummary(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
