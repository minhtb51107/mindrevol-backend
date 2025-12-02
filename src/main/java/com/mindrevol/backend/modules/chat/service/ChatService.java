package com.mindrevol.backend.modules.chat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.user.entity.User;

public interface ChatService {
    MessageResponse sendMessage(SendMessageRequest request, User sender);
    
    Page<MessageResponse> getConversationMessages(Long partnerId, User currentUser, Pageable pageable);
    
    // Hàm mới
    void markAsRead(Long partnerId, User currentUser);
}