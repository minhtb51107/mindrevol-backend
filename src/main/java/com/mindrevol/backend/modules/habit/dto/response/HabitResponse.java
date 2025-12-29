package com.mindrevol.backend.modules.habit.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class HabitResponse {
    // [FIX] Đổi tất cả UUID thành Long
    private Long id;          
    private Long userId;      
    private Long journeyId;   

    private String title;
    private String description;
    private String frequency;
    private LocalTime reminderTime;
    
    private boolean isCompletedToday;
}