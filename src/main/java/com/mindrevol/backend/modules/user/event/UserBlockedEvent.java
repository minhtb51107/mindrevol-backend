package com.mindrevol.backend.modules.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserBlockedEvent {
    private final Long blockerId;
    private final Long blockedId;
}