package com.mindrevol.backend.modules.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingEvent {
    private String conversationId; // [SỬA] Long -> String
    private String senderId;       // [SỬA] Long -> String
    private String receiverId;     // [SỬA] Long -> String
    private boolean isTyping; 
}