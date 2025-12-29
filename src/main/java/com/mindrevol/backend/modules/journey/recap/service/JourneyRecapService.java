package com.mindrevol.backend.modules.journey.recap.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JourneyRecapService {
    // [FIX] UUID -> Long
    Page<CheckinResponse> getUserRecapFeed(Long journeyId, User currentUser, Pageable pageable);
}