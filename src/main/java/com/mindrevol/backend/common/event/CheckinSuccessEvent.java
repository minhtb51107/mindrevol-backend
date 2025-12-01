package com.mindrevol.backend.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CheckinSuccessEvent {
    private UUID checkinId;
    private Long userId;
    private UUID journeyId;
    private LocalDateTime checkedInAt;
}