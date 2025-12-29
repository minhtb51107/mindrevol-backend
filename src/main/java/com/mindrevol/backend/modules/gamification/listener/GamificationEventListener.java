package com.mindrevol.backend.modules.gamification.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationEventListener {

    private final GamificationService gamificationService;
    private final CheckinRepository checkinRepository;

    @Async
    @EventListener
    @Transactional
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        // [FIX] event.getCheckinId() bây giờ là Long
        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        
        if (checkin != null) {
            gamificationService.awardPointsForCheckin(checkin);
        }
    }
}