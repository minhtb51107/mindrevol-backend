package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import com.mindrevol.backend.modules.journey.entity.JourneyStatus; // [MỚI] Import Enum
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
    
    private JourneyStatus journeyStatus; // [MỚI] Thêm trường này để FE biết trạng thái hành trình
    
    private LocalDateTime sentAt;
}