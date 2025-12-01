package com.mindrevol.backend.modules.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

import com.mindrevol.backend.modules.chat.entity.MessageType;

@Data
public class SendMessageRequest {
    @NotNull
    private Long receiverId; // Gửi cho ai?

    private String content;  // Nội dung (có thể null nếu gửi ảnh)
    
    private MessageType type = MessageType.TEXT;
    
    private String mediaUrl; // Link ảnh/voice nếu có

    private UUID replyToCheckinId; // ID ảnh check-in nếu đang reply
}