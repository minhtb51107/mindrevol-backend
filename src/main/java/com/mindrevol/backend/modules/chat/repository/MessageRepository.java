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
     * [SỬA LỖI]: Trả về Optional<Message> thay vì Optional<User>
     */
    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /**
     * Đếm số tin nhắn chưa đọc trong 1 hội thoại (dành cho User hiện tại)
     * Logic: Tin nhắn không phải của mình gửi (senderId != me) VÀ trạng thái chưa SEEN
     */
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation.id = :convId " +
           "AND m.senderId <> :currentUserId " +
           "AND m.deliveryStatus <> 'SEEN'")
    long countUnreadMessages(@Param("convId") Long convId, @Param("currentUserId") Long currentUserId);
}