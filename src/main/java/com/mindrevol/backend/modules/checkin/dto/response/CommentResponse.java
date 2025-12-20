package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID; // Import UUID

@Data
@Builder
public class CommentResponse {
    private UUID id; // <--- Đổi từ Long sang UUID để khớp với Entity CheckinComment
    private String content;
    private LocalDateTime createdAt;
    
    // Thông tin người bình luận
    private Long userId;
    private String userFullName;
    private String userAvatar;
}