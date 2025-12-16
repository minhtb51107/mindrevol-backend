package com.mindrevol.backend.modules.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.chat.entity.Conversation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
	
    // TỐI ƯU: Chỉ tìm theo đúng thứ tự ID nhỏ trước, lớn sau.
    // Logic sắp xếp sẽ nằm ở Service. Query này tận dụng Index (user1_id, user2_id) cực nhanh.
    @Query("SELECT c FROM Conversation c WHERE c.user1.id = :uid1 AND c.user2.id = :uid2")
    Optional<Conversation> findBySortedIds(@Param("uid1") Long uid1, @Param("uid2") Long uid2);
    
 // Tìm hội thoại 1-1 giữa 2 người
    @Query("SELECT c FROM Conversation c WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) OR (c.user1.id = :userId2 AND c.user2.id = :userId1)")
    Optional<Conversation> findConversationByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Lấy danh sách hội thoại của User, sắp xếp mới nhất lên đầu
    @Query("SELECT c FROM Conversation c WHERE c.user1.id = :userId OR c.user2.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByUser(@Param("userId") Long userId);
}