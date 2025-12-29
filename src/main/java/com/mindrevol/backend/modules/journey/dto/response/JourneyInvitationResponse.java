package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class JourneyInvitationResponse {
    private Long id;
    private Long journeyId; // [FIX] Đổi UUID -> Long
    private String journeyName;
    private String inviterName;
    private String inviterAvatar;
    private JourneyInvitationStatus status;
    private LocalDateTime sentAt;
}