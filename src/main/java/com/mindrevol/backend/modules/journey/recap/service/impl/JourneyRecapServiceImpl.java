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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JourneyRecapServiceImpl implements JourneyRecapService {

    private final CheckinRepository checkinRepository;
    private final JourneyRepository journeyRepository;
    private final CheckinMapper checkinMapper;
    private final ReactionService reactionService;

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getUserRecapFeed(UUID journeyId, User currentUser, Pageable pageable) {
        // 1. Kiểm tra hành trình có tồn tại không
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

        // 2. Query lấy danh sách bài đăng của chính user đó trong hành trình này
        Page<Checkin> myCheckins = checkinRepository.findMyCheckinsInJourney(journeyId, currentUser.getId(), pageable);

        // 3. Map sang Response và bổ sung thông tin Reaction (Tim/Like)
        return myCheckins.map(checkin -> {
            CheckinResponse response = checkinMapper.toResponse(checkin);
            
            // Lấy danh sách reaction preview (để hiện avatar người like giống Instagram)
            List<CheckinReactionDetailResponse> reactions = reactionService.getPreviewReactions(checkin.getId());
            response.setLatestReactions(reactions);
            
            // Mapper đã tự động map commentCount rồi (nếu bạn đã cấu hình @Formula hoặc count trong mapper)
            return response;
        });
    }

    // --- BỎ HẾT CÁC HÀM XỬ LÝ VIDEO/SLIDE CŨ ĐI ---
}