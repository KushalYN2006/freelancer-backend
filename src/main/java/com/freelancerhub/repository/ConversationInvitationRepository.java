package com.freelancerhub.repository;

import com.freelancerhub.model.ConversationInvitation;
import com.freelancerhub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationInvitationRepository extends JpaRepository<ConversationInvitation, Integer> {

    Optional<ConversationInvitation> findFirstBySenderUserIdAndReceiverUserIdOrderByCreatedAtDesc(
            Integer senderId, Integer receiverId);

    List<ConversationInvitation> findByReceiverUserIdAndStatus(
            Integer receiverId, ConversationInvitation.InvitationStatus status);

    boolean existsBySenderUserIdAndReceiverUserIdAndStatus(
            Integer senderId, Integer receiverId, ConversationInvitation.InvitationStatus status);

    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.userId <> :userId
          AND EXISTS (
              SELECT 1 FROM ConversationInvitation i
              WHERE ((i.sender.userId = :userId AND i.receiver.userId = u.userId)
                  OR (i.receiver.userId = :userId AND i.sender.userId = u.userId))
                AND i.status = com.freelancerhub.model.ConversationInvitation.InvitationStatus.accepted
          )
        ORDER BY u.name ASC
    """)
    List<User> findAcceptedConversationPartners(@Param("userId") Integer userId);
}
