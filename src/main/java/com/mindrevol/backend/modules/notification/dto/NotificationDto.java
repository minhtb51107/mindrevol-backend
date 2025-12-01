package com.mindrevol.backend.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private String type;    // SOS, COMEBACK, NEW_CHECKIN
    private String title;
    private String message;
    private String senderAvatar;
    private Long senderId;
}