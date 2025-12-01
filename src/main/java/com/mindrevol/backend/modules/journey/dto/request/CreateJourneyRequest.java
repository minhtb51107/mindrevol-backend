package com.mindrevol.backend.modules.journey.dto.request;

import com.mindrevol.backend.modules.journey.entity.JourneyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateJourneyRequest {
    @NotBlank(message = "Tên hành trình không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Loại hành trình là bắt buộc")
    private JourneyType type; // HABIT hoặc ROADMAP

    private LocalDate startDate;
    private LocalDate endDate;
    private String theme;

    private List<JourneyTaskRequest> roadmapTasks;
}