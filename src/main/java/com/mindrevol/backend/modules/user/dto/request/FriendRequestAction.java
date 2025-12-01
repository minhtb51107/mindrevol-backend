package com.mindrevol.backend.modules.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendRequestAction {
    @NotNull(message = "Target User ID is required")
    private Long targetUserId;
}