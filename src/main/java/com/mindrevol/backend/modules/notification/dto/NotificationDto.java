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
    private String type;    
    private String title;
    private String message;
    private String senderAvatar;
    private String senderId; // [UUID] String
}