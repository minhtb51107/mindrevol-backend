package com.mindrevol.backend.modules.habit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalTime;

@Data
public class CreateHabitRequest {
    @NotBlank(message = "Tên thói quen không được để trống")
    private String title;
    
    private String description;
    private String frequency; // DAILY
    private LocalTime reminderTime;
}