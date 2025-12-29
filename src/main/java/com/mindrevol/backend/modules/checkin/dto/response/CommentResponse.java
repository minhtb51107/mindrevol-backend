package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id; // [FIX] UUID -> Long
    private String content;
    private LocalDateTime createdAt;
    
    private Long userId;
    private String userFullName;
    private String userAvatar;
}