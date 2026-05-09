package com.freelancerhub.repository;

import com.freelancerhub.model.Message;
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
        SELECT DISTINCT
            CASE
                WHEN m.sender.userId = :userId THEN m.receiver
                ELSE m.sender
            END
        FROM Message m
        WHERE m.sender.userId = :userId OR m.receiver.userId = :userId
    """)
    List<Object> findConversationPartners(@Param("userId") Integer userId);
}