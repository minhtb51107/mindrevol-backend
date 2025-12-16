package com.mindrevol.backend.modules.journey.dto.response;

import com.mindrevol.backend.modules.journey.entity.InteractionType;
import com.mindrevol.backend.modules.journey.entity.JourneyRole;
import com.mindrevol.backend.modules.journey.entity.JourneyStatus;
import com.mindrevol.backend.modules.journey.entity.JourneyType;
import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class JourneyResponse {
    private UUID id;
    private String name;
    private String description;
    private String inviteCode;
    private JourneyType type;
    private JourneyStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String theme;
    
    private Long creatorId; 
    private String creatorName;
    private String creatorAvatar;
    
    private int participantCount;
    private boolean isJoined; // Cái này có thể tính toán sau
    private String inviteUrl;
    
    // --- THÊM CÁC TRƯỜNG NÀY ĐỂ FIX LỖI SETTING ---
    private boolean hasStreak;
    private boolean requiresFreezeTicket;
    private boolean isHardcore;
    private boolean requireApproval;
    private InteractionType interactionType;
    private JourneyVisibility visibility;
    
    // Thêm Role để Frontend biết là Owner hay Member
    private JourneyRole role; 
}