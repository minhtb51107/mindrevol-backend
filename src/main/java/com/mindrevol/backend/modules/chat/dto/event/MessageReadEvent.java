package com.mindrevol.backend.modules.chat.dto.event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageReadEvent {
    private Long conversationId;
    private Long lastReadMessageId;
    private Long userIdWhoRead;
}