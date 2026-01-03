package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.RequestStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class JourneyRequestResponse {
    private String id;           // [FIX] Đổi Long -> String (UUID)
    private String userId;       // [FIX] Đổi Long -> String (UUID)
    private String fullname;
    private String avatarUrl;
    private String handle;
    private LocalDateTime requestedAt; // Tương ứng với createdAt
    private RequestStatus status;
}