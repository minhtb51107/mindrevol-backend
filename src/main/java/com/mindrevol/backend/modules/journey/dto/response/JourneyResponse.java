package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class JourneyResponse {
    private String id; 
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private JourneyVisibility visibility;
    private JourneyStatus status;
    private String inviteCode;

    private boolean requireApproval; 
    private String creatorId; 
    private int participantCount; 
    
    // [THÊM MỚI] ID của Không gian chứa hành trình này
    private String boxId; 
    
    private CurrentUserStatus currentUserStatus;
    
    private String themeColor;
    
    private String avatar;

    @Data
    @Builder
    public static class CurrentUserStatus {
        private String role;
        private int currentStreak;
        private int totalCheckins;
        private boolean hasCheckedInToday;
    }
}