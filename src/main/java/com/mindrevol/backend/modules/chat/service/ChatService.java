package com.mindrevol.backend.modules.chat.service;

import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    // [SỬA] String userId
    MessageResponse sendMessage(String userId, SendMessageRequest request);
    
    List<ConversationResponse> getUserConversations(String userId);
    
    // [SỬA] String conversationId
    Page<MessageResponse> getConversationMessages(String conversationId, Pageable pageable);
    
    // [SỬA] String userId, partnerId
    Page<MessageResponse> getMessagesWithUser(String userId, String partnerId, Pageable pageable);
    
    void markConversationAsRead(String conversationId, String userId);
    
    Conversation getConversationById(String id);

    ConversationResponse getOrCreateConversation(String senderId, String receiverId);
}