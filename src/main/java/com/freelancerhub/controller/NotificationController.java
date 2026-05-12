package com.freelancerhub.controller;

import com.freelancerhub.model.Notification;
import com.freelancerhub.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getNotifications(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Integer userId) {
        long count = notificationRepository.countByRecipientUserIdAndReadFlagFalse(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markRead(@PathVariable Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setReadFlag(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<?> markAllRead(@PathVariable Integer userId) {
        List<Notification> notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setReadFlag(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
