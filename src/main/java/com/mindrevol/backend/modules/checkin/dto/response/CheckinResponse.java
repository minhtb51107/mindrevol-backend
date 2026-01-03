package com.mindrevol.backend.modules.checkin.dto.response;

import com.mindrevol.backend.modules.checkin.entity.ActivityType;
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
    private String id;
    private String journeyId;
    
    // User info
    private String userId;
    private String userFullName;
    private String userAvatar;

    // Content
    private String imageUrl;
    private String thumbnailUrl;
    private String caption;
    
    // Context Info
    private String emotion;
    private ActivityType activityType;
    private String activityName;
    private String locationName;
    private List<String> tags;

    // Meta
    private CheckinStatus status;
    private CheckinVisibility visibility;
    private LocalDate checkinDate;
    private LocalDateTime createdAt;

    // Stats
    private Long commentCount;
    private Long reactionCount;
    private List<CheckinReactionDetailResponse> latestReactions;
}