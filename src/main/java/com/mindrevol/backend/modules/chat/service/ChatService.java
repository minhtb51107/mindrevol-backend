package com.mindrevol.backend.modules.chat.service;

import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {
    MessageResponse sendMessage(String senderId, SendMessageRequest request);
    List<ConversationResponse> getUserConversations(String userId);
    Page<MessageResponse> getConversationMessages(String conversationId, Pageable pageable);
    Page<MessageResponse> getMessagesWithUser(String currentUserId, String partnerId, Pageable pageable);
    void markConversationAsRead(String conversationId, String userId);
    Conversation getConversationById(String id);
    ConversationResponse getOrCreateConversation(String senderId, String receiverId);

    Conversation createBoxConversation(String boxId, String boxName, String creatorId);
    void updateBoxConversationInfo(String boxId, String newName);
    void addUserToBoxConversation(String boxId, String userId);
    void removeUserFromBoxConversation(String boxId, String userId);
    
    // [THÊM MỚI]
    ConversationResponse getBoxConversation(String boxId, String userId);
}