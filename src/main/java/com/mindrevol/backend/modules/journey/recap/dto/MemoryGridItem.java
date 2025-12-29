package com.mindrevol.backend.modules.journey.recap.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.checkin.entity.Emotion;

@Data
@Builder
public class MemoryGridItem {
    private Long checkinId;
    private String thumbnailUrl;
    private CheckinStatus status; // NORMAL, FAILED (Client làm xám), COMEBACK (Client làm sáng)
    private String emotion;     // Để hiện icon cảm xúc nhỏ
    private LocalDateTime createdAt;
    private int reactionCount;    // Số lượng tim
}