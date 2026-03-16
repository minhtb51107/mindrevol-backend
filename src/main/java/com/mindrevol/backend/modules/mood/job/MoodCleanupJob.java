package com.mindrevol.backend.modules.mood.job;

import com.mindrevol.backend.modules.mood.repository.MoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoodCleanupJob {

    private final MoodRepository moodRepository;

    /**
     * Dọn dẹp các Mood đã quá 24h.
     * Chạy định kỳ vào phút thứ 0 của mỗi giờ (VD: 1:00, 2:00, 3:00...).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredMoods() {
        log.info("Bắt đầu dọn dẹp các Mood (Trạng thái) đã hết hạn...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // [VÁ LỖ HỔNG 3] Dùng Hard Delete bằng Native Query để database không bị phình to bởi rác.
            // 1. Phải xóa Reaction trước để không vướng khóa ngoại (Foreign Key constraint)
            moodRepository.hardDeleteExpiredReactions(now);
            
            // 2. Sau đó mới xóa Mood
            moodRepository.hardDeleteExpiredMoods(now);
            
            log.info("Dọn dẹp thành công các Mood đã hết hạn.");
        } catch (Exception e) {
            log.error("Có lỗi xảy ra trong quá trình dọn dẹp Mood", e);
        }
    }
}