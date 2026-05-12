package com.freelancerhub.controller;

import com.freelancerhub.model.Message;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.MessageRepository;
import com.freelancerhub.repository.UserRepository;
import com.freelancerhub.utils.ApiResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MessageController.java
 * Handles all REST API endpoints for the messaging system.
 *
 * Endpoints:
 *   POST  /api/messages                       → send a message
 *   GET   /api/messages/{senderId}/{receiverId} → get conversation between two users
 *   GET   /api/messages/inbox/{userId}          → get user's conversation list (inbox)
 */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    // ── POST /api/messages ────────────────────────────────────────────────────
    /**
     * Send a new message from one user to another.
     * Request body: { "senderId": 1, "receiverId": 2, "message": "Hello!" }
     */
    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body) {
        try {
            Integer senderId   = (Integer) body.get("senderId");
            Integer receiverId = (Integer) body.get("receiverId");
            String  text       = (String)  body.get("message");

            // Validate required fields
            if (senderId == null || receiverId == null || text == null || text.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "senderId, receiverId, and message are required"));
            }

            // Prevent messaging yourself
            if (senderId.equals(receiverId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "You cannot send a message to yourself"));
            }

            // Look up sender and receiver in Users table
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + senderId));

            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverId));

            // Build and save the message — INSERT INTO Messages ...
            Message message = new Message();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setMessage(text.trim());
            message.setTimestamp(LocalDateTime.now());

            Message saved = messageRepository.save(message);

            return ResponseEntity.ok(Map.of(
                    "message",   "Message sent",
                    "messageId", saved.getMessageId(),
                    "timestamp", saved.getTimestamp().toString()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/messages/{senderId}/{receiverId} ─────────────────────────────
    /**
     * Fetches the full conversation between two users in chronological order.
     * Both users can call this — it returns messages in both directions.
     * Used to render the chat window.
     */
    @GetMapping("/{senderId}/{receiverId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Integer senderId,
            @PathVariable Integer receiverId) {
        try {
            // Verify both users exist
            if (!userRepository.existsById(senderId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sender not found: " + senderId));
            }
            if (!userRepository.existsById(receiverId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Receiver not found: " + receiverId));
            }

            // Fetch all messages between these two users, sorted by timestamp ASC
            List<Message> conversation = messageRepository.findConversation(senderId, receiverId);

            return ResponseEntity.ok(conversation.stream().map(ApiResponseMapper::messageSummary).toList());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch conversation: " + e.getMessage()));
        }
    }

    // ── GET /api/messages/inbox/{userId} ──────────────────────────────────────
    /**
     * Returns the list of all users this person has ever chatted with.
     * Used to populate the conversation list sidebar in the chat page.
     */
    @GetMapping("/inbox/{userId}")
    public ResponseEntity<?> getInbox(@PathVariable Integer userId) {
        try {
            if (!userRepository.existsById(userId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not found: " + userId));
            }

            List<Object> partners = messageRepository.findConversationPartners(userId);
            return ResponseEntity.ok(partners.stream()
                    .filter(User.class::isInstance)
                    .map(User.class::cast)
                    .map(ApiResponseMapper::userSummary)
                    .toList());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch inbox: " + e.getMessage()));
        }
    }
}
