package com.mindrevol.backend.modules.checkin.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.user.entity.User;


public interface CheckinService {

	CheckinResponse createCheckin(CheckinRequest request, User currentUser);

	Page<CheckinResponse> getJourneyFeed(UUID journeyId, Pageable pageable, User currentUser);

}
