package com.mindrevol.backend.modules.chat.repository;

import com.mindrevol.backend.modules.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// [FIX] Đổi UUID -> Long để khớp với Entity Conversation
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    // Tìm hội thoại 1-1 chính xác (không quan tâm thứ tự user1/user2)
    @Query("SELECT c FROM Conversation c WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) OR (c.user1.id = :userId2 AND c.user2.id = :userId1)")
    Optional<Conversation> findByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // [QUAN TRỌNG] Lấy danh sách hội thoại nhưng LOẠI BỎ người bị chặn/chặn mình
    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.user1 u1 " +
           "JOIN FETCH c.user2 u2 " +
           "WHERE (u1.id = :userId OR u2.id = :userId) " +
           "AND NOT EXISTS ( " +
               "SELECT 1 FROM UserBlock ub " +
               "WHERE (ub.blocker.id = :userId AND (ub.blocked.id = u1.id OR ub.blocked.id = u2.id)) " + // Tôi chặn họ
               "OR (ub.blocked.id = :userId AND (ub.blocker.id = u1.id OR ub.blocker.id = u2.id)) " +   // Họ chặn tôi
           ") " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findValidConversationsByUserId(@Param("userId") Long userId);
    
    // Hàm cũ
    List<Conversation> findByUser1IdOrUser2IdOrderByLastMessageAtDesc(Long user1Id, Long user2Id);
}