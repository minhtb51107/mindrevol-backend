package com.mindrevol.backend.modules.journey.dto.request;

import lombok.Data;

@Data
public class UpdateJourneySettingsRequest {
    private String name;
    private String description;
    private String theme;
    
    // Các cấu hình luật chơi (Dùng Wrapper Class Boolean để có thể null nếu không muốn update)
    private Boolean hasStreak;
    private Boolean requiresFreezeTicket;
    private Boolean isHardcore;
}