package com.mindrevol.backend.modules.chat.service;

import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    MessageResponse sendMessage(Long senderId, SendMessageRequest request);
    
    // [UPDATE] Trả về ConversationResponse
    List<ConversationResponse> getUserConversations(Long userId);
    
    Page<MessageResponse> getConversationMessages(Long conversationId, Pageable pageable);
    Page<MessageResponse> getMessagesWithUser(Long currentUserId, Long partnerId, Pageable pageable);
    void markConversationAsRead(Long conversationId, Long userId);
    Conversation getConversationById(Long id);

	ConversationResponse getOrCreateConversation(Long senderId, Long receiverId);
}