package com.mindrevol.backend.modules.journey.job;

import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class JourneyCleanupJob {

    private final JourneyRepository journeyRepository;
    private final RedissonClient redissonClient;

    /**
     * Chạy mỗi ngày vào lúc 00:01 sáng.
     * Nhiệm vụ: Đóng các hành trình đã hết hạn (endDate < hôm nay).
     */
    @Scheduled(cron = "0 * * * * ?") // Chạy mỗi phút
    @Transactional
    public void closeExpiredJourneys() {
        // Dùng Redisson Lock để tránh chạy trùng nếu deploy nhiều server (như DailyStreakResetJob)
        String lockKey = "job:daily_journey_cleanup";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Thử lấy lock, chờ 0s, giữ lock trong 5 phút
            if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                log.info("Starting Daily Journey Cleanup Job...");

                LocalDate today = LocalDate.now();
                int updatedCount = journeyRepository.updateExpiredJourneysStatus(today);

                if (updatedCount > 0) {
                    log.info("Completed cleaning up: {} journeys marked as COMPLETED.", updatedCount);
                } else {
                    log.info("No expired journeys found today.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock interrupted during journey cleanup", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}