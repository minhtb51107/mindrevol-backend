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
        log.info("Event received: Checkin {}", event.getCheckinId());

        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        // Gọi service xử lý toàn bộ logic game (bao gồm cả cộng điểm và streak)
        // Lưu ý: Logic cộng điểm đã được bạn chuyển vào trong hàm processCheckinGamification ở bước trước rồi
        gamificationService.processCheckinGamification(checkin);
    }
}