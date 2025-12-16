package com.mindrevol.backend.modules.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingEvent {
    private Long conversationId;
    private Long senderId;
    private Long receiverId; // Gửi cho ai
    private boolean isTyping; // true = đang gõ, false = đã dừng
}