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
    private final GamificationService gamificationService; // Để trừ điểm/vé

    /**
     * Chạy mỗi giờ vào phút thứ 5 (00:05, 01:05, ..., 23:05).
     * Mục đích: Xử lý cho các User vừa bước qua ngày mới ở múi giờ của họ.
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void processDailyStreakLogic() {
        String lockKey = "job:hourly_streak_process";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Lock ngắn hơn (10 phút) vì chạy mỗi giờ
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
        // 1. Lấy giờ hiện tại theo UTC
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
        int currentHourUtc = nowUtc.getHour();

        // 2. Tìm danh sách User cần xử lý
        // Logic: User ở múi giờ X sẽ được xử lý khi (X_Hour + UTC_Hour) % 24 == 0 (Tức là 0h sáng giờ địa phương)
        // Tuy nhiên, query DB theo timezone string rất phức tạp.
        // Cách tối ưu: Quét theo Batch và check giờ địa phương trong Code (Application Level).
        // Để tránh full scan table mỗi giờ, ta có thể tối ưu query sau (nhưng ở đây làm cách an toàn trước).
        
        // CÁCH ĐƠN GIẢN HIỆU QUẢ:
        // Query những JourneyParticipant có currentStreak > 0 VÀ lastCheckinAt < (Hôm nay của họ).
        // Nhưng làm sao biết "Hôm nay của họ"?
        // -> Ta sẽ query tất cả participant active, sau đó filter trong vòng lặp.
        
        int batchSize = 200;
        Pageable pageable = PageRequest.of(0, batchSize);
        boolean hasNext = true;

        // Lưu ý: Query này nên được tối ưu thêm index ở DB.
        // Tạm thời lấy những người có streak > 0 để check.
        while (hasNext) {
            hasNext = processBatch(pageable);
            pageable = pageable.next();
        }
    }

    @Transactional
    public boolean processBatch(Pageable pageable) {
        // Query: Lấy user có streak > 0 để kiểm tra xem đã qua ngày chưa
        // Cần thêm method này vào Repository: findByCurrentStreakGreaterThan(0, pageable)
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

        // Ngày hiện tại theo giờ địa phương của User
        LocalDate todayLocal = LocalDate.now(zoneId);
        
        // Ngày check-in cuối cùng
        LocalDate lastCheckin = p.getLastCheckinAt();

        // Nếu chưa check-in bao giờ thì bỏ qua (hoặc xử lý riêng)
        if (lastCheckin == null) return;

        // LOGIC CHÍNH:
        // Nếu hôm nay là ngày T, check-in cuối là T-1 -> An toàn, chưa cần làm gì (đợi tối nhắc).
        // Nếu hôm nay là ngày T, check-in cuối là T-2 (hoặc cũ hơn) -> ĐÃ MẤT CHUỖI CỦA NGÀY T-1.
        // Ta cần xử lý ngay khi vừa bước sang ngày T (tức là vừa qua 0h sáng ngày T).
        
        // Ví dụ: Checkin cuối ngày 01/01. 
        // Bây giờ là 00:05 ngày 03/01 (User vừa qua ngày 02 mà không làm). -> Mất chuỗi.
        // Đợi chút, logic đúng là: Checkin cuối 01/01. 
        // 00:05 ngày 02/01 -> Vẫn còn cơ hội làm trong ngày 02 -> Chưa reset.
        // 00:05 ngày 03/01 -> Đã hết ngày 02 mà chưa làm -> Reset.
        
        long daysGap = java.time.temporal.ChronoUnit.DAYS.between(lastCheckin, todayLocal);

        if (daysGap >= 2) { 
            // Đã lỡ ít nhất 1 ngày trọn vẹn (ngày hôm qua)
            
            // --- AUTO FREEZE LOGIC ---
            boolean savedByFreeze = false;
            
            // Nếu Journey yêu cầu vé VÀ user có vé
            if (p.getJourney().isRequiresFreezeTicket() && user.getFreezeStreakCount() > 0) {
                log.info("User {} missed check-in. Attempting auto-freeze...", user.getId());
                
                // 1. Trừ vé
                user.setFreezeStreakCount(user.getFreezeStreakCount() - 1);
                userRepository.save(user); // Lưu user update vé
                
                // 2. Cập nhật ngày check-in thành "Hôm qua" (để lấp lỗ hổng)
                // Coi như hôm qua đã check-in bằng vé nghỉ
                p.setLastCheckinAt(todayLocal.minusDays(1));
                // Streak giữ nguyên (không tăng, không giảm)
                participantRepository.save(p);
                
                // 3. Ghi log point history (nếu cần tracking vé)
                // (Optional: gamificationService.recordFreezeUsage(user));

                // 4. Thông báo
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

            // --- NẾU KHÔNG CỨU ĐƯỢC -> RESET ---
            if (!savedByFreeze) {
                int oldStreak = p.getCurrentStreak();
                p.setCurrentStreak(0);
                participantRepository.save(p);

                notificationService.sendAndSaveNotification(
                        user.getId(),
                        null,
                        NotificationType.STREAK_LOST,
                        "Rất tiếc, chuỗi đã đứt! 💔",
                        "Bạn đã lỡ check-in. Chuỗi " + oldStreak + " ngày đã về 0.",
                        p.getJourney().getId().toString(),
                        null
                );
            }
        }
    }
}