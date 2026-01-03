package com.mindrevol.backend.modules.journey.recap.dto;

import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MemoryGridItem {
    private String checkinId; // [UUID] String
    private String thumbnailUrl;
    private CheckinStatus status;
    private String emotion;
    private LocalDateTime createdAt;
    private int reactionCount;
}