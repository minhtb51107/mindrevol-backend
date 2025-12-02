package com.mindrevol.backend.modules.notification.job;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    public void remindUsersToCheckin() {
        log.info("Starting Check-in Reminder Job...");

        LocalDate today = LocalDate.now();
        int batchSize = 100;
        Pageable pageable = PageRequest.of(0, batchSize);
        
        boolean hasNext = true;
        
        // Loop qua các trang dữ liệu
        while (hasNext) {
            hasNext = processReminderBatch(today, pageable);
            // Lưu ý: Vì job này CHỈ ĐỌC và gửi noti (không sửa dữ liệu query), 
            // nên ta phải tăng page index thủ công nếu dùng Pageable thông thường.
            // Tuy nhiên, logic query là "lastCheckin < today", khi gửi noti xong thì condition vẫn đúng.
            // Nên ta cần tăng pageNumber lên.
            pageable = pageable.next(); 
        }

        log.info("Reminder Job completed.");
    }
    
    @Transactional
    public boolean processReminderBatch(LocalDate today, Pageable pageable) {
        // Query tối ưu: Chỉ lấy người CHƯA check-in
        Slice<JourneyParticipant> slice = participantRepository.findParticipantsToRemind(today, pageable);
        List<JourneyParticipant> participants = slice.getContent();
        
        if (participants.isEmpty()) {
            return false;
        }

        for (JourneyParticipant p : participants) {
            try {
                String title;
                String message;

                if (p.getJourney().isHardcore()) {
                    title = "Sắp hết ngày rồi! 😱";
                    message = "Bạn chưa check-in cho hành trình " + p.getJourney().getName() + ". Đừng để mất chuỗi nhé!";
                } else {
                    title = "Chia sẻ khoảnh khắc nào! 📸";
                    message = "Mọi người trong " + p.getJourney().getName() + " đang chờ tin bạn đấy!";
                }

                notificationService.sendAndSaveNotification(
                        p.getUser().getId(),
                        null,
                        NotificationType.CHECKIN_REMINDER,
                        title,
                        message,
                        p.getJourney().getId().toString(),
                        null
                );
            } catch (Exception e) {
                log.error("Failed to send reminder to user {}", p.getUser().getId());
            }
        }
        
        return slice.hasNext();
    }
}