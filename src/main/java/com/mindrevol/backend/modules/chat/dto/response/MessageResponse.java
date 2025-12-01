package com.mindrevol.backend.modules.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import com.mindrevol.backend.modules.chat.entity.MessageType;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    
    private String content;
    private MessageType type;
    private String mediaUrl;
    
    private boolean isRead;
    private LocalDateTime createdAt;

    // Ngữ cảnh (Context)
    private UUID replyToCheckinId;
    private String replyToCheckinThumbnail; // Link ảnh nhỏ để hiện trong khung chat
}