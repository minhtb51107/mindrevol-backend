package com.mindrevol.backend.modules.chat.dto.event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageReadEvent {
    private String conversationId;    // [SỬA] Long -> String
    private String lastReadMessageId; // [SỬA] Long -> String
    private String userIdWhoRead;     // [SỬA] Long -> String
}