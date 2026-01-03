package com.mindrevol.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinSuccessEvent {
    // [FIX] Đổi toàn bộ Long -> String để khớp với UUID
    private String checkinId;
    private String userId;
    private String journeyId;
    private LocalDateTime checkinTime;
}