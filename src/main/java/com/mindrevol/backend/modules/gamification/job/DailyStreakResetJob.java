package com.mindrevol.backend.modules.gamification.job;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType;
import com.mindrevol.backend.modules.notification.service.NotificationService;
import com.mindrevol.backend.modules.user.entity.User;
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
public class DailyStreakResetJob {

    private final JourneyParticipantRepository participantRepository;
    private final NotificationService notificationService;

    // Chạy lúc 00:05 sáng mỗi ngày (Delay 5p để chắc chắn qua ngày mới)
    @Scheduled(cron = "0 5 0 * * ?") 
    @Transactional
    public void resetStreaks() {
        log.info("Starting Daily Streak Reset Job...");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // Logic: Duyệt tất cả participant, ai mà lastCheckinDate < yesterday nghĩa là hôm qua KHÔNG check-in
        // => Reset streak về 0
        List<JourneyParticipant> participants = participantRepository.findAll();

        for (JourneyParticipant p : participants) {
            
            // Nếu chưa từng check-in hoặc check-in lần cuối trước ngày hôm qua
            // Ví dụ: Hôm nay 21/10. Yesterday 20/10.
            // Nếu lastCheckin = 19/10 => Missed 20/10 => Reset.
            boolean missedYesterday = p.getLastCheckinAt() == null || p.getLastCheckinAt().isBefore(yesterday);
            
            if (missedYesterday && p.getCurrentStreak() > 0) {
                int oldStreak = p.getCurrentStreak();
                
                // 1. Reset Streak
                p.setCurrentStreak(0);
                participantRepository.save(p);
                
                // 2. Gửi thông báo AN ỦI cho chính chủ
                notificationService.sendAndSaveNotification(
                        p.getUser().getId(),
                        null,
                        NotificationType.STREAK_LOST,
                        "Ôi không, chuỗi đã mất! 😢",
                        "Bạn đã lỡ check-in hôm qua. Chuỗi " + oldStreak + " ngày đã về 0. Hãy bắt đầu lại ngay hôm nay nhé!",
                        p.getJourney().getId().toString(),
                        null
                );

                // 3. (Optional) Gửi thông báo cho BẠN BÈ trong nhóm để vào AN ỦI
                // Tìm các thành viên khác trong cùng Journey
                notifyFriendsToComfort(p.getJourney().getId(), p.getUser(), oldStreak);
                
                log.info("Reset streak for user {} in journey {}", p.getUser().getId(), p.getJourney().getId());
            }
        }
        
        log.info("Streak Reset Job completed.");
    }

    private void notifyFriendsToComfort(java.util.UUID journeyId, User failedUser, int lostStreak) {
        // Logic: Lấy danh sách thành viên khác trong journey (trừ người failed)
        List<JourneyParticipant> friends = participantRepository.findAllByJourneyId(journeyId);
        
        for (JourneyParticipant friend : friends) {
            if (!friend.getUser().getId().equals(failedUser.getId())) {
                notificationService.sendAndSaveNotification(
                        friend.getUser().getId(),
                        failedUser.getId(),
                        NotificationType.STREAK_LOST, // Client sẽ hiện icon "Failed" để an ủi
                        failedUser.getFullname() + " vừa mất chuỗi " + lostStreak + " ngày 😭",
                        "Hãy gửi lời động viên để bạn ấy quay trở lại nào!",
                        journeyId.toString(),
                        failedUser.getAvatarUrl()
                );
            }
        }
    }
}