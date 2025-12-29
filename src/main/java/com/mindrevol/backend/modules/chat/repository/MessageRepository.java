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

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Lấy danh sách tin nhắn của hội thoại (phân trang)
    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * Tìm tin nhắn mới nhất trong hội thoại
     */
    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /**
     * Đếm số tin nhắn chưa đọc trong 1 hội thoại (dành cho User hiện tại)
     * Logic: Tin nhắn không phải của mình gửi (sender.id != me) VÀ trạng thái chưa SEEN
     */
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :convId " +
           "AND m.sender.id <> :currentUserId " +  // [FIX] sender.id thay vì senderId
           "AND m.deliveryStatus <> 'SEEN'")
    long countUnreadMessages(@Param("convId") Long convId, @Param("currentUserId") Long currentUserId);

    /**
     * [MỚI] Tìm danh sách các tin nhắn chưa đọc để cập nhật status
     * Lấy các tin trong hội thoại, không phải do mình gửi, và status khác SEEN
     */
    @Query("SELECT m FROM Message m " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.sender.id <> :userId " +          // [FIX] sender.id thay vì senderId
           "AND m.deliveryStatus <> :status")
    List<Message> findUnreadMessagesInConversation(
        @Param("conversationId") Long conversationId, 
        @Param("userId") Long userId, 
        @Param("status") MessageDeliveryStatus status
    );
}