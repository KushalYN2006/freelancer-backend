package com.freelancerhub.repository;

import com.freelancerhub.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Integer userId);

    long countByRecipientUserIdAndReadFlagFalse(Integer userId);
}
