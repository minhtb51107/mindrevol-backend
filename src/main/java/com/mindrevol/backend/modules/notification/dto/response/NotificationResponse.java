package com.mindrevol.backend.modules.notification.dto.response;

import com.mindrevol.backend.modules.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id; // [UUID] String
    private String title;
    private String message;
    private NotificationType type;
    private String referenceId;
    private String imageUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
    
    // [UUID] Sender ID là String
    private String senderId;
    private String senderName;
}