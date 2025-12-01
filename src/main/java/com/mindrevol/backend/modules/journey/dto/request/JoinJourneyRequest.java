package com.mindrevol.backend.modules.journey.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinJourneyRequest {
    @NotBlank(message = "Mã mời không được để trống")
    private String inviteCode;
}