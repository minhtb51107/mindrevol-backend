package com.mindrevol.backend.modules.chat.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.service.ChatService;
import com.mindrevol.backend.modules.user.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // Gửi tin nhắn (Chat thường HOẶC Reply ảnh)
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        MessageResponse response = chatService.sendMessage(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Lấy lịch sử chat với 1 người cụ thể
    @GetMapping("/history/{partnerId}")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getHistory(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<MessageResponse> response = chatService.getConversationMessages(partnerId, currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PatchMapping("/read/{partnerId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal User currentUser) {
        
        chatService.markAsRead(partnerId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}