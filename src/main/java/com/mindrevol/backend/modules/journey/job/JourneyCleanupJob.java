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
     * PRODUCTION CONFIG:
     * Chạy 1 lần mỗi ngày vào lúc 00:01:00 (1 phút sau nửa đêm).
     * Lý do: Để đảm bảo LocalDate.now() đã chắc chắn chuyển sang ngày mới.
     * Cron: "Giây Phút Giờ Ngày Tháng Thứ"
     */
    @Scheduled(cron = "0 1 0 * * ?") 
    @Transactional
    public void closeExpiredJourneys() {
        String lockKey = "job:daily_journey_cleanup";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Thử lấy lock, không chờ (0), giữ lock trong 30 phút (đề phòng job chạy lâu)
            if (lock.tryLock(0, 30, TimeUnit.MINUTES)) {
                log.info("⏰ Starting Daily Journey Cleanup Job (Midnight Scan)...");

                LocalDate today = LocalDate.now();
                
                // Batch update: Cực nhanh và nhẹ
                int updatedCount = journeyRepository.updateExpiredJourneysStatus(today);

                if (updatedCount > 0) {
                    log.info("✅ Cleanup complete: Closed {} expired journeys.", updatedCount);
                } else {
                    log.info("💤 No expired journeys found today.");
                }
            } else {
                log.info("Job execution skipped (Locked by another instance).");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Job interrupted", e);
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}