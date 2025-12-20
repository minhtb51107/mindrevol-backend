package com.mindrevol.backend.modules.checkin.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.CheckinVisibility;

@Data
@Builder
public class CheckinResponse {
    private UUID id;
    private String userId;
    private String userFullName;
    private String userAvatar;
    private String imageUrl;
    private String thumbnailUrl;
    private String caption;
    private String emotion;
    private CheckinStatus status;
    private CheckinVisibility visibility;
    
    private String taskTitle;
    // --- THÊM LẠI CÁC FIELD BỊ THIẾU ---
    private UUID taskId;
    private Integer taskDayNo;
    // ------------------------------------
    
    private LocalDateTime createdAt;
    
    private long commentCount;
    private long reactionCount;
    
    // Field mới cho FacePile
    private List<CheckinReactionDetailResponse> latestReactions; 
}