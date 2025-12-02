package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    
    // Thông tin người bình luận
    private Long userId;
    private String userFullName;
    private String userAvatar;
}