package com.mindrevol.backend.modules.notification.job;

import com.mindrevol.backend.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    // Chạy vào lúc 02:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Bắt đầu dọn dẹp các thông báo cũ...");
        
        // Xóa các thông báo ĐÃ ĐỌC và cũ hơn 30 ngày
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        try {
            notificationRepository.deleteOldReadNotifications(cutoffDate);
            log.info("Đã dọn dẹp xong thông báo cũ hơn 30 ngày.");
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp thông báo: ", e);
        }
    }
}