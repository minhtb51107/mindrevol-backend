package com.mindrevol.backend.modules.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.chat.entity.Conversation;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
	
	@Query("SELECT c FROM Conversation c WHERE (c.user1.id = :u1 AND c.user2.id = :u2) OR (c.user1.id = :u2 AND c.user2.id = :u1)")
    Optional<Conversation> findByUsers(Long u1, Long u2);
    
    @Query("SELECT c FROM Conversation c WHERE (c.user1.id = :u1 AND c.user2.id = :u2) OR (c.user1.id = :u2 AND c.user2.id = :u1)")
    Optional<Conversation> findConversationByUsers(Long u1, Long u2);
}