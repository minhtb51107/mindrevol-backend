package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CheckinReactionDetailResponse {
    private Long id; // [FIX] UUID -> Long
    private Long userId;
    private String userFullName;
    private String userAvatar;
    
    private String type; 
    private String emoji; 
    private String content; 
    
    private String mediaUrl;
    private LocalDateTime createdAt;
}