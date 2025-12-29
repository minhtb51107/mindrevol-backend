package com.mindrevol.backend.modules.journey.dto.request;

import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateJourneyRequest {

    @NotBlank(message = "Tên hành trình không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @FutureOrPresent(message = "Ngày kết thúc phải ở tương lai")
    private LocalDate endDate;

    private JourneyVisibility visibility = JourneyVisibility.PUBLIC;
    
    private boolean requireApproval = false;
}