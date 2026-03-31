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
public interface MessageRepository extends JpaRepository<Message, String> {
    
    // Lấy danh sách tin nhắn của một cuộc hội thoại, sắp xếp mới nhất lên đầu (hỗ trợ phân trang)
    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    // Lấy đúng 1 tin nhắn mới nhất của cuộc hội thoại (Dùng để hiển thị nội dung preview ngoài danh sách Inbox)
    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(String conversationId);

    // Đếm số lượng tin nhắn "Chưa đọc" (trạng thái khác SEEN) được gửi từ người khác cho user hiện tại
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :convId " +
           "AND m.sender.id <> :currentUserId " +
           "AND m.deliveryStatus <> 'SEEN'")
    long countUnreadMessages(@Param("convId") String convId, @Param("currentUserId") String currentUserId);

    // Lấy danh sách các Entity tin nhắn chưa đọc để hệ thống có thể update trạng thái chúng thành "Đã đọc" (SEEN)
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