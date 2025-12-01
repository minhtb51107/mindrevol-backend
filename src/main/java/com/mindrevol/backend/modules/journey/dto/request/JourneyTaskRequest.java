package com.mindrevol.backend.modules.journey.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JourneyTaskRequest {
    
    @NotNull(message = "Ngày thứ mấy là bắt buộc")
    @Min(value = 1, message = "Ngày phải bắt đầu từ 1")
    private Integer dayNo;      // Ngày 1, Ngày 2...

    @NotBlank(message = "Tiêu đề nhiệm vụ không được để trống")
    private String title;       // Tiêu đề: Học 5 từ vựng

    private String description; // Mô tả: Chủ đề gia đình
}