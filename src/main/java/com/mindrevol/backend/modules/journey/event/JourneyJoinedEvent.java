package com.mindrevol.backend.modules.journey.event;

import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JourneyJoinedEvent {
    private final Journey journey;
    private final User participant;
}