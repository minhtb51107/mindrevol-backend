package com.mindrevol.backend.modules.habit.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class HabitResponse {
    private UUID id;
    
    private Long userId;
    private UUID journeyId;
    
    private String title;
    private String description;
    private String frequency;
    private LocalTime reminderTime;
    private boolean isCompletedToday;
}