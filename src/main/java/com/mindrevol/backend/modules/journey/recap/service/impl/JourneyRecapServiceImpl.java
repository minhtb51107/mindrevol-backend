package com.mindrevol.backend.modules.journey.recap.service.impl;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.service.ReactionService;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.recap.service.JourneyRecapService;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JourneyRecapServiceImpl implements JourneyRecapService {

    private final CheckinRepository checkinRepository;
    private final JourneyRepository journeyRepository;
    private final CheckinMapper checkinMapper;
    private final ReactionService reactionService;

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getUserRecapFeed(String journeyId, User currentUser, Pageable pageable) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        // [UUID] findMyCheckinsInJourney Repository đã nhận String ID
        Page<Checkin> myCheckins = checkinRepository.findMyCheckinsInJourney(journeyId, currentUser.getId(), pageable);

        return myCheckins.map(checkin -> {
            CheckinResponse response = checkinMapper.toResponse(checkin);
            List<CheckinReactionDetailResponse> reactions = reactionService.getPreviewReactions(checkin.getId());
            response.setLatestReactions(reactions);
            return response;
        });
    }
}