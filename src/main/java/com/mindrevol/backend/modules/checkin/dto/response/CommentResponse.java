package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private String id; // [FIX] Long -> String (UUID)
    private String content;
    private LocalDateTime createdAt;
    
    private String userId; // [FIX] Long -> String (UUID)
    private String userFullName;
    private String userAvatar;
}