package com.mindrevol.backend.modules.chat.service;

import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service xử lý logic nhắn tin và quản lý cuộc trò chuyện.
 */
public interface ChatService {
    // Gửi tin nhắn mới
    MessageResponse sendMessage(String senderId, SendMessageRequest request);
    
    // Lấy danh sách các cuộc trò chuyện của người dùng (Inbox)
    List<ConversationResponse> getUserConversations(String userId);
    
    // Lấy lịch sử tin nhắn của một cuộc trò chuyện cụ thể
    Page<MessageResponse> getConversationMessages(String conversationId, Pageable pageable);
    
    // Lấy lịch sử tin nhắn 1-1 giữa người dùng hiện tại và đối tác
    Page<MessageResponse> getMessagesWithUser(String currentUserId, String partnerId, Pageable pageable);
    
    // Đánh dấu cuộc trò chuyện là đã đọc
    void markConversationAsRead(String conversationId, String userId);
    
    // Tìm entity Conversation theo ID
    Conversation getConversationById(String id);
    
    // Lấy cuộc trò chuyện 1-1 hiện có hoặc tạo mới nếu chưa từng chat
    ConversationResponse getOrCreateConversation(String senderId, String receiverId);

    // Tạo phòng chat chung cho một Box
    Conversation createBoxConversation(String boxId, String boxName, String creatorId);
    
    // Cập nhật thông tin phòng chat Box khi Box thay đổi
    void updateBoxConversationInfo(String boxId, String newName);
    
    // Thêm người dùng vào phòng chat của Box khi họ tham gia Box
    void addUserToBoxConversation(String boxId, String userId);
    
    // Xóa người dùng khỏi phòng chat Box khi họ rời Box
    void removeUserFromBoxConversation(String boxId, String userId);
    
    // [THÊM MỚI] Lấy thông tin cuộc trò chuyện của một Box cụ thể
    ConversationResponse getBoxConversation(String boxId, String userId);
}