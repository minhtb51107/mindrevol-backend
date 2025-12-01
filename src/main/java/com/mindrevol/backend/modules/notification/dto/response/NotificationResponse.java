package com.mindrevol.backend.modules.notification.dto.response;

import com.mindrevol.backend.modules.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private String referenceId;
    private String imageUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
    
    // Thông tin rút gọn người gửi (để hiển thị avatar/tên nếu cần)
    private Long senderId;
    private String senderName;
}