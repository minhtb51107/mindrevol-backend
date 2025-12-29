package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JourneyParticipantResponse {
    private Long id; // Participant ID
    private UserSummaryResponse user;
    private String role;
    private LocalDateTime joinedAt;
    
    // Chỉ số gamification tối giản
    private int currentStreak;
    private int totalCheckins;
    private LocalDateTime lastCheckinAt;
}