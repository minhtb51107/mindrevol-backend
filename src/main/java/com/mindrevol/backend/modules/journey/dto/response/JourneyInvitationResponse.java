package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class JourneyInvitationResponse {
    private String id;        // [UUID] String
    private String journeyId; // [UUID] String
    private String journeyName;
    private String inviterName;
    private String inviterAvatar;
    private JourneyInvitationStatus status;
    private LocalDateTime sentAt;
}