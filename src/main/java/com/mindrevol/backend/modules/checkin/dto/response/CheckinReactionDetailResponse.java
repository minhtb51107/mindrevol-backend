package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CheckinReactionDetailResponse {
    private String id;
    private String userId; // UUID String
    private String userFullName;
    private String userAvatar;
    
    // [THÊM MỚI] Để phân biệt Reaction hay Comment
    private String type;    // "REACTION" hoặc "COMMENT"
    private String content; // Nội dung comment (nếu có)
    
    private String emoji;
    private String mediaUrl;
    private LocalDateTime createdAt;
}