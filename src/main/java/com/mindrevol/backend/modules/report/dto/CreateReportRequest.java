package com.mindrevol.backend.modules.report.dto;

import com.mindrevol.backend.modules.report.entity.ReportReason;
import com.mindrevol.backend.modules.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "Target ID is required")
    private String targetId;

    @NotNull(message = "Target type is required")
    private ReportTargetType targetType;

    @NotNull(message = "Reason is required")
    private ReportReason reason;

    private String description;
}