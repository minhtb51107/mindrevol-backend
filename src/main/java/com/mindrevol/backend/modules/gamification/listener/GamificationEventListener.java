package com.mindrevol.backend.modules.gamification.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.gamification.enabled", havingValue = "true")
public class GamificationEventListener {

    private final GamificationService gamificationService;
    private final CheckinRepository checkinRepository;

    @Async
    // @EventListener  <-- ĐÃ TẮT: Ngưng lắng nghe sự kiện check-in để không tính điểm/chuỗi
    @Transactional
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        log.info("Event received: Checkin {}", event.getCheckinId());

        /* * TẠM DỪNG TÍNH NĂNG GAMIFICATION
         * Code cũ vẫn giữ để tham khảo, nhưng không thực thi.
         
        Checkin checkin = checkinRepository.findById(event.getCheckinId()).orElse(null);
        if (checkin == null) return;

        gamificationService.processCheckinGamification(checkin);
        */
    }
}