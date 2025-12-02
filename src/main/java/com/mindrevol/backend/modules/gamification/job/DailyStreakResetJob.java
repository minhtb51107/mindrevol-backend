package com.mindrevol.backend.modules.gamification.job;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
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
public class DailyStreakResetJob {

    private final JourneyParticipantRepository participantRepository;
    private final NotificationService notificationService;

    // Chạy lúc 00:05 sáng mỗi ngày
    @Scheduled(cron = "0 5 0 * * ?")
    public void resetStreaks() {
        log.info("Starting Daily Streak Reset Job...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int batchSize = 100; // Xử lý mỗi lần 100 user
        Pageable pageable = PageRequest.of(0, batchSize);
        
        boolean hasNext = true;

        while (hasNext) {
            // Chúng ta phải thực hiện trong transaction nhỏ để commit dữ liệu
            // Nếu lỗi ở batch này thì không ảnh hưởng batch khác
            hasNext = processBatch(yesterday, pageable);
        }

        log.info("Streak Reset Job completed.");
    }

    @Transactional // Transaction nằm ở mức Batch nhỏ
    public boolean processBatch(LocalDate yesterday, Pageable pageable) {
        // Query tối ưu: Chỉ lấy những người CẦN reset
        Slice<JourneyParticipant> slice = participantRepository.findParticipantsToResetStreak(yesterday, pageable);
        List<JourneyParticipant> participants = slice.getContent();

        if (participants.isEmpty()) {
            return false;
        }

        for (JourneyParticipant p : participants) {
            int oldStreak = p.getCurrentStreak();

            // 1. Reset Streak
            p.setCurrentStreak(0);
            // Không cần gọi save(p) vì đang trong @Transactional, Hibernate tự detect thay đổi.
            // Nhưng gọi save() cũng không sao để tường minh.
            participantRepository.save(p);

            // 2. Gửi thông báo AN ỦI (Chỉ gửi thông báo, logic gửi mail/push nên là async)
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

                // 3. Gửi thông báo cho bạn bè (Cân nhắc: Nếu friend list quá lớn, phần này nên đẩy vào Queue riêng)
                notifyFriendsToComfort(p.getJourney().getId(), p.getUser(), oldStreak);
                
            } catch (Exception e) {
                log.error("Error sending notification for user {}", p.getUser().getId(), e);
                // Catch lỗi để không làm rollback việc reset streak
            }
        }
        
        return slice.hasNext();
    }

    private void notifyFriendsToComfort(java.util.UUID journeyId, User failedUser, int lostStreak) {
        // Lưu ý: Logic này vẫn có rủi ro nếu 1 nhóm có 1000 người.
        // Tạm thời giữ nguyên logic cũ của bạn, nhưng về sau nên move vào Message Queue.
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