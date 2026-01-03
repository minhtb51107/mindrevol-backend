package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class JourneyResponse {
    private String id; // [UUID] String
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private JourneyVisibility visibility;
    private JourneyStatus status;
    private String inviteCode;

    // [FIX] Thêm trường này để sửa lỗi compilation
    private boolean requireApproval; 

    // [FIX] Thêm creatorId để frontend xác định Owner
    private String creatorId; 
    
    // [FIX] Đổi tên từ totalMembers -> participantCount cho khớp với Frontend
    private int participantCount; 
    
    private CurrentUserStatus currentUserStatus;

    @Data
    @Builder
    public static class CurrentUserStatus {
        private String role;
        private int currentStreak;
        private int totalCheckins;
        private boolean hasCheckedInToday;
    }
}