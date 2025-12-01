package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import com.mindrevol.backend.modules.journey.entity.JourneyType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class JourneyResponse {
    private UUID id;
    private String name;
    private String description;
    private String inviteCode;
    private JourneyType type;
    private JourneyStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String theme;
    
    private Long creatorId; 
    private String creatorName;
    private String creatorAvatar;
    
    private int participantCount;
    private boolean isJoined;
    private String inviteUrl;
    private String qrCodeData;
}