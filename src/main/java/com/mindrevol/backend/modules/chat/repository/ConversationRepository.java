package com.mindrevol.backend.modules.chat.repository;

import com.mindrevol.backend.modules.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    @Query("SELECT c FROM Conversation c " +
           "WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) " +
           "OR (c.user1.id = :userId2 AND c.user2.id = :userId1) " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByUsers(@Param("userId1") String userId1, @Param("userId2") String userId2);

    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.user1 u1 " +
           "JOIN FETCH c.user2 u2 " +
           "WHERE (u1.id = :userId OR u2.id = :userId) " +
           "AND c.boxId IS NULL " + // Lọc riêng chỉ lấy chat 1-1
           "AND NOT EXISTS ( " +
               "SELECT 1 FROM UserBlock ub " +
               "WHERE (ub.blocker.id = :userId AND (ub.blocked.id = u1.id OR ub.blocked.id = u2.id)) " +
               "OR (ub.blocked.id = :userId AND (ub.blocker.id = u1.id OR ub.blocker.id = u2.id)) " +
           ") " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findValidConversationsByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM Conversation c WHERE c.boxId = :boxId")
    Optional<Conversation> findByBoxId(@Param("boxId") String boxId);

    // [THÊM MỚI] Lấy danh sách các Group Chat của Box mà User tham gia
    @Query("SELECT c FROM Conversation c WHERE c.boxId IN " +
           "(SELECT bm.box.id FROM BoxMember bm WHERE bm.user.id = :userId)")
    List<Conversation> findBoxConversationsByUserId(@Param("userId") String userId);
}