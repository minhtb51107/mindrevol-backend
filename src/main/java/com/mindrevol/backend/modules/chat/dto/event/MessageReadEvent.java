package com.mindrevol.backend.modules.chat.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReadEvent {
    private Long conversationId;
    private Long readerId; // Người vừa đọc tin (là currentUser)
    private Long partnerId; // Người được báo tin (người gửi tin nhắn gốc)
    private String readAt; // Thời điểm đọc
}