package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CheckinReactionDetailResponse {
    private UUID id;
    private Long userId;
    private String userFullName;
    private String userAvatar;
    
    private String type; // "REACTION" hoặc "COMMENT"
    private String emoji; // Nếu là REACTION thì có cái này
    private String content; // Nếu là COMMENT thì có cái này
    
    private String mediaUrl;
    private LocalDateTime createdAt;
}