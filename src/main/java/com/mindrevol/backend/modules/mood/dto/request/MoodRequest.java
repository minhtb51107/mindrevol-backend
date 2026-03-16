package com.mindrevol.backend.modules.mood.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MoodRequest {
    @NotBlank(message = "Icon không được để trống")
    private String icon;
    private String message;
}