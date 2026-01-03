package com.mindrevol.backend.modules.habit.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class HabitResponse {
    // [UUID] Đổi tất cả sang String
    private String id;          
    private String userId;      
    private String journeyId;   

    private String title;
    private String description;
    private String frequency;
    private LocalTime reminderTime;
    
    private boolean isCompletedToday;
}