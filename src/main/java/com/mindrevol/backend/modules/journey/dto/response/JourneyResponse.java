package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class JourneyResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private JourneyVisibility visibility;
    private JourneyStatus status;
    private String inviteCode;
    
    // Thông tin của User hiện tại trong Journey này (nếu có)
    private CurrentUserStatus currentUserStatus;

    // Thống kê nhanh
    private int totalMembers;
    
    @Data
    @Builder
    public static class CurrentUserStatus {
        private String role; // OWNER / MEMBER
        private int currentStreak;
        private int totalCheckins;
        private boolean hasCheckedInToday;
    }
}