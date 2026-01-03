package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class JourneyParticipantResponse {
    private String id;
    private UserSummaryResponse user;
    private String role;
    private LocalDateTime joinedAt;
    
    private int currentStreak;
    private int totalCheckins;
    private LocalDateTime lastCheckinAt;

    private int totalActiveDays;
    private double presenceRate;
    private String activityPersona;

    // Logic tính % đơn giản: Active Days / Days Joined
    public static double calculatePresenceRate(int activeDays, LocalDateTime joinedAt) {
        if (joinedAt == null) return 0.0;
        
        long daysJoined = ChronoUnit.DAYS.between(joinedAt, LocalDateTime.now()) + 1;
        if (daysJoined <= 0) daysJoined = 1;
        
        if (activeDays > daysJoined) activeDays = (int) daysJoined;

        double rate = ((double) activeDays / daysJoined) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }
}