package com.mindrevol.backend.modules.gamification.job;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.gamification.enabled", havingValue = "true")
public class DailyStreakResetJob {

    private final JourneyParticipantRepository participantRepository;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 5 * * * ?") 
    public void processDailyStreakLogic() {
        String lockKey = "job:hourly_streak_process";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(0, 10, TimeUnit.MINUTES)) {
                log.info("Starting Hourly Streak Process Job...");
                executeJobLogic();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
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
        Slice<JourneyParticipant> slice = participantRepository.findAll(pageable); 
        List<JourneyParticipant> participants = slice.getContent();
        
        if (participants.isEmpty()) return false;

        for (JourneyParticipant p : participants) {
            try {
                if (p.getCurrentStreak() > 0) { 
                    processSingleParticipant(p);
                }
            } catch (Exception e) {
                log.error("Error streak participant {}", p.getId(), e);
            }
        }
        return slice.hasNext();
    }

    private void processSingleParticipant(JourneyParticipant p) {
        User user = p.getUser();
        ZoneId zoneId = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "UTC");
        LocalDate todayLocal = LocalDate.now(zoneId);
        LocalDate lastCheckin = p.getLastCheckinAt() != null ? p.getLastCheckinAt().toLocalDate() : null;

        if (lastCheckin == null) return;

        // Gap >= 2 (Vd: Checkin ngày 1, nay ngày 3 -> Miss ngày 2)
        long daysGap = java.time.temporal.ChronoUnit.DAYS.between(lastCheckin, todayLocal);

        if (daysGap >= 2) { 
            // [HARDCORE MODE] Không cứu vãn, không đóng băng. Mất là mất.
            int oldStreak = p.getCurrentStreak();
            
            p.setCurrentStreak(0);
            participantRepository.save(p);

            // Gửi thông báo chia buồn
            notificationService.sendAndSaveNotification(
                    user.getId(), null, NotificationType.STREAK_LOST,
                    "Chuỗi đã đứt! 💔",
                    "Bạn đã lỡ check-in và mất chuỗi " + oldStreak + " ngày. Hãy bắt đầu lại nào!",
                    p.getJourney().getId().toString(), null
            );
            
            log.info("Reset streak for User {} in Journey {} (Was: {})", user.getId(), p.getJourney().getId(), oldStreak);
        }
    }
}