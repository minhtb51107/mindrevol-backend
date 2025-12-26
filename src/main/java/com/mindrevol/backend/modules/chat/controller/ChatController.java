package com.mindrevol.backend.modules.chat.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 1. Gửi tin nhắn
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(@RequestBody SendMessageRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(userId, request)));
    }

    // 2. Lấy danh sách Inbox (Đã nâng cấp trả về DTO xịn)
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getUserConversations(userId)));
    }

    // 3. Lấy tin nhắn chi tiết
    @GetMapping("/messages/{partnerId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable Long partnerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessagesWithUser(userId, partnerId, pageable)));
    }

    // 4. Đánh dấu đã đọc
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long conversationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/conversations/init/{receiverId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @PathVariable Long receiverId) {
        Long senderId = SecurityUtils.getCurrentUserId();
        ConversationResponse response = chatService.getOrCreateConversation(senderId, receiverId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}