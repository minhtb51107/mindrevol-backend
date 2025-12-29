package com.mindrevol.backend.modules.journey.dto.request;

import com.mindrevol.backend.modules.journey.entity.JourneyVisibility;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateJourneySettingsRequest {
    private String name;
    private String description;
    private LocalDate endDate;
    private JourneyVisibility visibility;
    private Boolean requireApproval;
}