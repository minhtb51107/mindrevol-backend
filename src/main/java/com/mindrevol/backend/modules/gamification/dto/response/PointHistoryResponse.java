package com.mindrevol.backend.modules.gamification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PointHistoryResponse {
    private Long amount;
    private String reason;
    private LocalDateTime createdAt;
}