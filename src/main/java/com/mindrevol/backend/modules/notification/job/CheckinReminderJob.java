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
import java.time.LocalDateTime;
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

        // Lấy mốc thời gian bắt đầu ngày hôm nay (00:00:00)
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        
        int batchSize = 100;
        Pageable pageable = PageRequest.of(0, batchSize);
        
        boolean hasNext = true;
        
        while (hasNext) {
            hasNext = processReminderBatch(startOfToday, pageable);
            pageable = pageable.next(); 
        }

        log.info("Reminder Job completed.");
    }
    
    @Transactional
    public boolean processReminderBatch(LocalDateTime startOfToday, Pageable pageable) {
        // Query tìm người chưa check-in kể từ đầu ngày
        Slice<JourneyParticipant> slice = participantRepository.findParticipantsToRemind(startOfToday, pageable);
        List<JourneyParticipant> participants = slice.getContent();
        
        if (participants.isEmpty()) {
            return false;
        }

        for (JourneyParticipant p : participants) {
            try {
                // [FIX] Bỏ logic Hardcore, dùng thông báo chung thân thiện
                String title = "Đừng quên kỷ niệm hôm nay! 📸";
                String message = "Bạn chưa check-in cho hành trình " + p.getJourney().getName() + ". Hãy lưu giữ khoảnh khắc trước khi ngày trôi qua nhé!";

                notificationService.sendAndSaveNotification(
                        p.getUser().getId(),
                        null, // System notification
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