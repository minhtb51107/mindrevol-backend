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
        List<JourneyParticipant> participants = participantRepository.findAll();

        for (JourneyParticipant p : participants) {
            if (p.getLastCheckinAt() == null || p.getLastCheckinAt().isBefore(today)) {
                
                String title;
                String message;

                // --- LOGIC MỚI: DÙNG CẤU HÌNH ---
                if (p.getJourney().isHardcore()) {
                    // Chế độ Kỷ luật
                    title = "Sắp hết ngày rồi! 😱";
                    message = "Bạn chưa check-in cho hành trình " + p.getJourney().getName() + ". Đừng để mất chuỗi nhé!";
                } else {
                    // Chế độ Vui vẻ (Giải trí/Công việc)
                    title = "Chia sẻ khoảnh khắc nào! 📸";
                    message = "Mọi người trong " + p.getJourney().getName() + " đang chờ tin bạn đấy!";
                }
                // --------------------------------
                
                notificationService.sendAndSaveNotification(
                        p.getUser().getId(),
                        null,
                        NotificationType.CHECKIN_REMINDER,
                        title,
                        message,
                        p.getJourney().getId().toString(),
                        null
                );
            }
        }
        
        log.info("Reminder Job completed.");
    }
}