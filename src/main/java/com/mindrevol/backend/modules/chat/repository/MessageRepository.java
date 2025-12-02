package com.mindrevol.backend.modules.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.chat.entity.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    // Tìm tin nhắn trong hội thoại này, mà người gửi KHÔNG PHẢI là tôi (tức là tin đến), và chưa đọc
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.sender.id <> :currentUserId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("convId") Long convId, @Param("currentUserId") Long currentUserId);
}