package com.mindrevol.backend.modules.journey.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class InviteFriendRequest {
    @NotNull(message = "Journey ID is required")
    private UUID journeyId;

    @NotNull(message = "Friend ID is required")
    private Long friendId;
}