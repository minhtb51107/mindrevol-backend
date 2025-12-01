package com.mindrevol.backend.modules.gamification.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
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

    @Async // Quan trọng: Chạy ở luồng riêng (không làm chậm user check-in)
    @EventListener
    @Transactional
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        log.info("Nhận sự kiện Check-in thành công: user={}, journey={}", event.getUserId(), event.getJourneyId());

        // Lấy lại thông tin Checkin đầy đủ từ DB (nếu cần xử lý phức tạp)
        Checkin checkin = checkinRepository.findById(event.getCheckinId())
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        // Gọi service tính điểm, streak, badge...
        gamificationService.processCheckinGamification(checkin);
        
        log.info("Đã xử lý xong Gamification cho checkin: {}", event.getCheckinId());
    }
}