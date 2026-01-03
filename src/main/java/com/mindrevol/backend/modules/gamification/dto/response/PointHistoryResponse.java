package com.mindrevol.backend.modules.gamification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PointHistoryResponse {
    private String id; // [UUID] String
    private int amount;
    private String source;
    private String description;
    private LocalDateTime createdAt;
}