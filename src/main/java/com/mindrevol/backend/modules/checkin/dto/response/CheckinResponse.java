package com.mindrevol.backend.modules.checkin.dto.response;

import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CheckinResponse {
    private Long id;          // [FIX] UUID -> Long
    private Long journeyId;   // [FIX] UUID -> Long
    
    // User info
    private Long userId;
    private String userFullName;
    private String userAvatar;

    // Content
    private String imageUrl;
    private String caption;
    private String emotion;
    private CheckinStatus status;
    private CheckinVisibility visibility;
    private LocalDate checkinDate;
    private LocalDateTime createdAt;

    // Stats
    private Long commentCount;
    private Long reactionCount;
    private List<CheckinReactionDetailResponse> latestReactions;
    
    // [ĐÃ XÓA] taskId, taskTitle, taskDayNo
}