package com.mindrevol.backend.modules.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserBlockedEvent {
    // [UUID] Đổi Long -> String
    private String blockerId;
    private String blockedId;
}