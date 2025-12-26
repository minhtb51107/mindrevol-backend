package com.mindrevol.backend.modules.journey.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JourneyRequestResponse {
    private UUID requestId;
    private Long userId;
    private String fullname;
    private String avatarUrl;
    private String handle;
    private LocalDateTime createdAt;
}