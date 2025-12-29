package com.mindrevol.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CheckinSuccessEvent {
    // [FIX] Đổi UUID -> Long
    private final Long checkinId;
    private final Long userId;
    private final Long journeyId;
    private final LocalDateTime checkinTime;
}