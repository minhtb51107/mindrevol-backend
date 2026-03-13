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

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(@RequestBody SendMessageRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(userId, request)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getUserConversations(userId)));
    }

    @GetMapping("/messages/{partnerId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable String partnerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessagesWithUser(userId, partnerId, pageable)));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String conversationId) {
        String userId = SecurityUtils.getCurrentUserId();
        chatService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/conversations/init/{receiverId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @PathVariable String receiverId) {
        String senderId = SecurityUtils.getCurrentUserId();
        ConversationResponse response = chatService.getOrCreateConversation(senderId, receiverId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // [THÊM MỚI] Lấy thông tin Group Chat từ Box ID
    @GetMapping("/conversations/box/{boxId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getBoxConversation(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        ConversationResponse response = chatService.getBoxConversation(boxId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
 // [THÊM MỚI] Lấy tin nhắn theo ID cuộc trò chuyện (Hỗ trợ tốt cho cả Box Chat)
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<Page<MessageResponse>> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponse.success(chatService.getConversationMessages(conversationId, pageable));
    }
}