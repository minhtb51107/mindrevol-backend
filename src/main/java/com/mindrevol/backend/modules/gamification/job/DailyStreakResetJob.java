package com.mindrevol.backend.modules.gamification.job;

import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStreakResetJob {

    private final JourneyParticipantRepository participantRepository;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient;
    private final UserRepository userRepository;
    private final GamificationService gamificationService; 

    /**
     * Chạy mỗi giờ vào phút thứ 5 (00:05, 01:05, ..., 23:05).
     * Mục đích: Xử lý cho các User vừa bước qua ngày mới ở múi giờ của họ.
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void processDailyStreakLogic() {
        String lockKey = "job:hourly_streak_process";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                log.info("Starting Hourly Streak Process Job...");
                executeJobLogic();
                log.info("Hourly Streak Process completed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void executeJobLogic() {
        int batchSize = 200;
        Pageable pageable = PageRequest.of(0, batchSize);
        boolean hasNext = true;

        while (hasNext) {
            hasNext = processBatch(pageable);
            pageable = pageable.next();
        }
    }

    @Transactional
    public boolean processBatch(Pageable pageable) {
        // Query: Lấy user có streak > 0 để kiểm tra xem đã qua ngày chưa
        Slice<JourneyParticipant> slice = participantRepository.findByCurrentStreakGreaterThan(0, pageable);
        List<JourneyParticipant> participants = slice.getContent();

        if (participants.isEmpty()) {
            return false;
        }

        for (JourneyParticipant p : participants) {
            try {
                processSingleParticipant(p);
            } catch (Exception e) {
                log.error("Error processing streak for participant {}", p.getId(), e);
            }
        }
        return slice.hasNext();
    }

    private void processSingleParticipant(JourneyParticipant p) {
        User user = p.getUser();
        String timezoneId = user.getTimezone() != null ? user.getTimezone() : "UTC";
        
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezoneId);
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }

        LocalDate todayLocal = LocalDate.now(zoneId);
        LocalDate lastCheckin = p.getLastCheckinAt();

        if (lastCheckin == null) return;

        // Logic: Nếu hôm nay là T, check-in cuối là T-2 (hoặc cũ hơn) -> ĐÃ MẤT CHUỖI
        long daysGap = java.time.temporal.ChronoUnit.DAYS.between(lastCheckin, todayLocal);

        if (daysGap >= 2) { 
            // --- AUTO FREEZE LOGIC ---
            boolean savedByFreeze = false;
            
            if (p.getJourney().isRequiresFreezeTicket() && user.getFreezeStreakCount() > 0) {
                log.info("User {} missed check-in. Attempting auto-freeze...", user.getId());
                
                // 1. Trừ vé
                user.setFreezeStreakCount(user.getFreezeStreakCount() - 1);
                userRepository.save(user); 
                
                // 2. Coi như hôm qua đã check-in (bằng vé)
                p.setLastCheckinAt(todayLocal.minusDays(1));
                participantRepository.save(p);
                
                // 3. Thông báo
                notificationService.sendAndSaveNotification(
                    user.getId(),
                    null,
                    NotificationType.STREAK_SAVED,
                    "Chuỗi đã được bảo vệ! ❄️",
                    "Bạn quên check-in hôm qua, hệ thống đã tự động dùng 1 Vé đóng băng để giữ chuỗi.",
                    p.getJourney().getId().toString(),
                    null
                );
                
                savedByFreeze = true;
            }

            // --- NẾU KHÔNG CỨU ĐƯỢC -> RESET VÀ LƯU BACKUP ---
            if (!savedByFreeze) {
                int oldStreak = p.getCurrentStreak();
                
                // [MỚI] Lưu lại chuỗi cũ để cho phép user "Sửa sai"
                p.setSavedStreak(oldStreak);
                
                p.setCurrentStreak(0);
                participantRepository.save(p);

                notificationService.sendAndSaveNotification(
                        user.getId(),
                        null,
                        NotificationType.STREAK_LOST,
                        "Rất tiếc, chuỗi đã đứt! 💔",
                        "Bạn đã lỡ check-in. Bạn có 48h để dùng vé 'Sửa Chuỗi' để khôi phục lại " + oldStreak + " ngày.",
                        p.getJourney().getId().toString(),
                        null
                );
            }
        }
    }
}