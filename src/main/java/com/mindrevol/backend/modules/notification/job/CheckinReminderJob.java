package com.mindrevol.backend.modules.notification.job;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckinReminderJob {

    private final JourneyParticipantRepository participantRepository;
    private final NotificationService notificationService;

    // Chạy vào lúc 20:00 mỗi ngày
    @Scheduled(cron = "0 0 20 * * ?") 
    @Transactional(readOnly = true)
    public void remindUsersToCheckin() {
        log.info("Starting Check-in Reminder Job...");

        LocalDate today = LocalDate.now();

        // 1. Lấy tất cả người tham gia (Lưu ý: Với lượng user lớn, nên dùng Pagination hoặc Batch Processing)
        // Query này cần tối ưu trong Repository: tìm những người mà lastCheckinDate < today
        // Ở đây tôi giả định dùng findAll() cho MVP, bạn nên tối ưu query sau.
        List<JourneyParticipant> participants = participantRepository.findAll();

        for (JourneyParticipant p : participants) {
            // Nếu check-in lần cuối TRƯỚC ngày hôm nay => Hôm nay chưa làm
            if (p.getLastCheckinAt() == null || p.getLastCheckinAt().isBefore(today)) {
                
                String title = "Sắp hết ngày rồi! 😱";
                String message = "Bạn chưa check-in cho hành trình " + p.getJourney().getName() + ". Đừng để mất chuỗi nhé!";
                
                // Gửi thông báo
                notificationService.sendAndSaveNotification(
                        p.getUser().getId(),
                        null, // System sender
                        NotificationType.CHECKIN_REMINDER,
                        title,
                        message,
                        p.getJourney().getId().toString(), // Reference ID để click vào mở Journey
                        null
                );
            }
        }
        
        log.info("Reminder Job completed.");
    }
}