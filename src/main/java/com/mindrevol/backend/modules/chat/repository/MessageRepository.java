package com.mindrevol.backend.modules.chat.repository;

import com.mindrevol.backend.modules.chat.entity.Message;
import com.mindrevol.backend.modules.chat.entity.MessageDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// [SỬA] JpaRepository<Message, String>
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    // [SỬA] conversationId là String
    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);

    // [SỬA] Tham số String
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :convId " +
           "AND m.sender.id <> :currentUserId " +
           "AND m.deliveryStatus <> 'SEEN'")
    long countUnreadMessages(@Param("convId") String convId, @Param("currentUserId") String currentUserId);

    // [SỬA] Tham số String
    @Query("SELECT m FROM Message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id <> :userId " +
           "AND m.deliveryStatus <> :status")
    List<Message> findUnreadMessagesInConversation(
        @Param("conversationId") String conversationId, 
        @Param("userId") String userId, 
        @Param("status") MessageDeliveryStatus status
    );
}