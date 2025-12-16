package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JourneyParticipantResponse {
    private Long userId;       // ID dùng để chuyển quyền
    private String fullname;
    private String avatarUrl;
    private String handle;
    private JourneyRole role;
}