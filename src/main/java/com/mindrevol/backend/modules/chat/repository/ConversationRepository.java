package com.mindrevol.backend.modules.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.chat.entity.Conversation;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
	
    // TỐI ƯU: Chỉ tìm theo đúng thứ tự ID nhỏ trước, lớn sau.
    // Logic sắp xếp sẽ nằm ở Service. Query này tận dụng Index (user1_id, user2_id) cực nhanh.
    @Query("SELECT c FROM Conversation c WHERE c.user1.id = :uid1 AND c.user2.id = :uid2")
    Optional<Conversation> findBySortedIds(@Param("uid1") Long uid1, @Param("uid2") Long uid2);
}