package com.mindrevol.backend.modules.gamification.job;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock; // <--- IMPORT
import org.redisson.api.RedissonClient; // <--- IMPORT
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStreakResetJob {

    private final JourneyParticipantRepository participantRepository;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient; // <--- Inject Client

    // Chạy lúc 00:05 sáng mỗi ngày
    @Scheduled(cron = "0 5 0 * * ?")
    public void resetStreaks() {
        // --- LOGIC DISTRIBUTED LOCK ---
        String lockKey = "job:daily_streak_reset";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Thử lấy lock, chờ 0s, giữ lock trong 30 phút
            // (Nếu server khác đang chạy rồi thì server này bỏ qua ngay)
            if (lock.tryLock(0, 30, TimeUnit.MINUTES)) {
                
                log.info("Acquired Lock. Starting Daily Streak Reset Job...");
                executeJobLogic();
                log.info("Streak Reset Job completed.");
                
            } else {
                log.info("Job is already running on another instance. Skipping.");
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

    // Tách logic chính ra hàm riêng
    private void executeJobLogic() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int batchSize = 100;
        Pageable pageable = PageRequest.of(0, batchSize);
        
        boolean hasNext = true;
        while (hasNext) {
            hasNext = processBatch(yesterday, pageable);
        }
    }

    @Transactional
    public boolean processBatch(LocalDate yesterday, Pageable pageable) {
        Slice<JourneyParticipant> slice = participantRepository.findParticipantsToResetStreak(yesterday, pageable);
        List<JourneyParticipant> participants = slice.getContent();

        if (participants.isEmpty()) {
            return false;
        }

        for (JourneyParticipant p : participants) {
            int oldStreak = p.getCurrentStreak();
            p.setCurrentStreak(0);
            participantRepository.save(p);

            try {
                notificationService.sendAndSaveNotification(
                        p.getUser().getId(),
                        null,
                        NotificationType.STREAK_LOST,
                        "Ôi không, chuỗi đã mất! 😢",
                        "Bạn đã lỡ check-in hôm qua. Chuỗi " + oldStreak + " ngày đã về 0. Hãy bắt đầu lại ngay hôm nay nhé!",
                        p.getJourney().getId().toString(),
                        null
                );
                notifyFriendsToComfort(p.getJourney().getId(), p.getUser(), oldStreak);
            } catch (Exception e) {
                log.error("Error sending notification for user {}", p.getUser().getId(), e);
            }
        }
        return slice.hasNext();
    }

    private void notifyFriendsToComfort(java.util.UUID journeyId, User failedUser, int lostStreak) {
        List<JourneyParticipant> friends = participantRepository.findAllByJourneyId(journeyId);
        for (JourneyParticipant friend : friends) {
            if (!friend.getUser().getId().equals(failedUser.getId())) {
                notificationService.sendAndSaveNotification(
                        friend.getUser().getId(),
                        failedUser.getId(),
                        NotificationType.STREAK_LOST,
                        failedUser.getFullname() + " vừa mất chuỗi " + lostStreak + " ngày 😭",
                        "Hãy gửi lời động viên để bạn ấy quay trở lại nào!",
                        journeyId.toString(),
                        failedUser.getAvatarUrl()
                );
            }
        }
    }
}