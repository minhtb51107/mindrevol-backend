package com.mindrevol.backend.modules.chat.dto.response;

import com.mindrevol.backend.modules.chat.entity.MessageDeliveryStatus;
import com.mindrevol.backend.modules.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    
    // Chỉ trả về ID, Frontend tự lookup thông tin user (Tối ưu performance)
    private Long senderId;
    private Long receiverId;
    
    private String content;
    private MessageType type;
    
    // Chứa thông tin reply check-in, ảnh, sticker... (Locket logic nằm trong này)
    private Map<String, Object> metadata;
    
    // ID tạm của client để xử lý Optimistic UI
    private String clientSideId;
    
    // Trạng thái: SENT, DELIVERED, SEEN
    private MessageDeliveryStatus deliveryStatus;
    
    private LocalDateTime createdAt;
}