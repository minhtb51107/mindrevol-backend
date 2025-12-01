package com.mindrevol.backend.modules.checkin.dto.response;

import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.Emotion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CheckinResponse {
    private UUID id;
    private String imageUrl;
    private String thumbnailUrl;
    private Emotion emotion;
    private CheckinStatus status;
    private String caption;
    private LocalDateTime createdAt;
    
    private Long userId;
    private String userAvatar;
    private String userFullName;

    private UUID taskId;      
    private String taskTitle;  
    private Integer taskDayNo; 
}