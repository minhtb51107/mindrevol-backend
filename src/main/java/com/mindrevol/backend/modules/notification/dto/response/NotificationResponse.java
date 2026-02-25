package com.mindrevol.backend.modules.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty; // Thêm import này
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private String type;
    private String referenceId;
    private String imageUrl;
    
    // [QUAN TRỌNG] Ép Spring Boot phải giữ nguyên tên "isRead" khi tạo JSON
    @JsonProperty("isRead")
    private boolean isRead;
    
    private LocalDateTime createdAt;
    private String senderId;
    private String senderName;
}