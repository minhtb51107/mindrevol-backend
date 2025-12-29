package com.mindrevol.backend.modules.chat.dto.response;

import com.mindrevol.backend.modules.chat.entity.MessageDeliveryStatus;
import com.mindrevol.backend.modules.chat.entity.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private String clientSideId;
    private Long conversationId;
    
    private Long senderId;
    private String senderAvatar; // [FIX] Thêm trường này để Mapper không báo lỗi
    
    private String content;
    private MessageType type;
    private Map<String, Object> metadata;
    
    private MessageDeliveryStatus deliveryStatus;
    private boolean isDeleted;
    private Long replyToMsgId;
    
    private LocalDateTime createdAt;
}