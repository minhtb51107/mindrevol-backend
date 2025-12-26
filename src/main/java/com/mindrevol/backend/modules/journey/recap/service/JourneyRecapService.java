package com.mindrevol.backend.modules.journey.recap.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.journey.recap.dto.JourneyRecapResponse;
import com.mindrevol.backend.modules.user.entity.User;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JourneyRecapService {

	Page<CheckinResponse> getUserRecapFeed(UUID journeyId, User currentUser, Pageable pageable);
//    JourneyRecapResponse generateRecap(User user, UUID journeyId);
}