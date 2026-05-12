package com.freelancerhub.repository;

import com.freelancerhub.model.Message;
import com.freelancerhub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * MessageRepository.java
 * Handles all database queries for the Messages table.
 */
public interface MessageRepository extends JpaRepository<Message, Integer> {

    /**
     * Fetch full conversation between two users in chronological order.
     * Gets messages where:
     *   (sender = A AND receiver = B) OR (sender = B AND receiver = A)
     * This gives the complete two-way chat history between the two users.
     */
    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.userId = :senderId AND m.receiver.userId = :receiverId)
           OR (m.sender.userId = :receiverId AND m.receiver.userId = :senderId)
        ORDER BY m.timestamp ASC
    """)
    List<Message> findConversation(
            @Param("senderId") Integer senderId,
            @Param("receiverId") Integer receiverId
    );

    /**
     * Get the list of users who have had any conversation with this user.
     * Used to populate the conversation sidebar (inbox list).
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.userId <> :userId
          AND EXISTS (
              SELECT 1 FROM Message m
              WHERE (m.sender.userId = :userId AND m.receiver.userId = u.userId)
                 OR (m.receiver.userId = :userId AND m.sender.userId = u.userId)
          )
        ORDER BY u.name ASC
    """)
    List<User> findConversationPartners(@Param("userId") Integer userId);

    boolean existsBySenderUserIdAndReceiverUserId(Integer senderId, Integer receiverId);
}
