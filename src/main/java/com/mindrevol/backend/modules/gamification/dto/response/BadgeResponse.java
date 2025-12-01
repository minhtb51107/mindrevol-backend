package com.mindrevol.backend.modules.gamification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BadgeResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private LocalDateTime earnedAt;
}