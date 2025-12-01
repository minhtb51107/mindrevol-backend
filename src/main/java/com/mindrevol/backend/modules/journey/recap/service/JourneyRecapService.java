package com.mindrevol.backend.modules.journey.recap.service;

import com.mindrevol.backend.modules.journey.recap.dto.JourneyRecapResponse;
import com.mindrevol.backend.modules.user.entity.User;
import java.util.UUID;

public interface JourneyRecapService {
    JourneyRecapResponse generateRecap(User user, UUID journeyId);
}