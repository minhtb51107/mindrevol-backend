package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.JourneyRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JourneyParticipantResponse {
    private Long userId;       
    private String fullname;
    private String avatarUrl;
    private String handle;
    private JourneyRole role;
    
    // [MỚI] Trạng thái trong ngày để hiển thị viền/icon
    private DailyMemberStatus status;
    
    // [MỚI] Thời gian check-in gần nhất (nếu có)
    private LocalDateTime lastCheckinTime;
}