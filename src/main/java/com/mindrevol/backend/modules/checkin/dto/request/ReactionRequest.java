package com.mindrevol.backend.modules.checkin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

import com.mindrevol.backend.modules.checkin.entity.ReactionType;

@Data
public class ReactionRequest {
    @NotNull(message = "ID bài check-in là bắt buộc")
    private UUID checkinId;

    @NotNull(message = "Loại reaction là bắt buộc")
    private ReactionType type;

    private String mediaUrl;
}