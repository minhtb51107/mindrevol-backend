package com.mindrevol.backend.modules.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BadgeResponse {
    private Long id;
    
    // --- Thêm field này để sửa lỗi .code() ---
    private String code; 
    
    private String name;
    private String description;
    private String iconUrl;
    private String conditionType;
    private Integer requiredValue;
    
    private boolean isOwned;
    
    // --- Frontend dùng obtainedAt, Backend (UserBadge) có earnedAt ---
    // Mapper sẽ lo việc chuyển đổi này, DTO giữ nguyên obtainedAt
    private LocalDateTime obtainedAt; 
}