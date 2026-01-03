package com.mindrevol.backend.modules.journey.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteFriendRequest {
    @NotNull(message = "Journey ID is required")
    private String journeyId; // [UUID] String

    @NotNull(message = "Friend ID is required")
    private String friendId; // [UUID] String
}