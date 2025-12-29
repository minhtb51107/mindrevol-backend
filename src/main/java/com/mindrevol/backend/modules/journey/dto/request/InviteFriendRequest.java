package com.mindrevol.backend.modules.journey.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteFriendRequest {
    @NotNull(message = "Journey ID is required")
    private Long journeyId; // [FIX] UUID -> Long

    @NotNull(message = "Friend ID is required")
    private Long friendId;
}