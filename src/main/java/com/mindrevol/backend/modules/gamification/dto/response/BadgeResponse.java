package com.mindrevol.backend.modules.gamification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BadgeResponse {
    private String id; // [UUID] String
    private String name;
    private String description;
    private String iconUrl;
    private LocalDateTime earnedAt; // Null nếu chưa đạt được (dùng cho danh sách all badges)
}