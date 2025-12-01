package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JourneyInvitationResponse {
    private Long id;
    private UUID journeyId;
    private String journeyName;
    private String inviterName; // Tên người mời
    private String inviterAvatar;
    private JourneyInvitationStatus status;
    private LocalDateTime sentAt;
}