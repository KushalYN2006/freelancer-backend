package com.freelancerhub.service;

import com.freelancerhub.model.Notification;
import com.freelancerhub.model.User;
import com.freelancerhub.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public Notification create(User recipient, User actor, Notification.NotificationType type,
                               String title, String message, String link) {
        if (recipient == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        return notificationRepository.save(notification);
    }
}
