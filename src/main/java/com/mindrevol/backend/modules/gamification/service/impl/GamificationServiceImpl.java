package com.mindrevol.backend.modules.gamification.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse; 
import com.mindrevol.backend.modules.gamification.entity.*;
import com.mindrevol.backend.modules.gamification.mapper.GamificationMapper;
import com.mindrevol.backend.modules.gamification.repository.BadgeRepository;
import com.mindrevol.backend.modules.gamification.repository.PointHistoryRepository;
import com.mindrevol.backend.modules.gamification.repository.UserBadgeRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.notification.entity.NotificationType; 
import com.mindrevol.backend.modules.notification.service.NotificationService; 
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict; // <--- IMPORT
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    // ... [Các trường inject giữ nguyên] ...
    private final JourneyParticipantRepository participantRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GamificationMapper gamificationMapper;

    @Value("${app.gamification.points.item-freeze-cost}")
    private int freezeItemCost;

    @Value("${app.gamification.points.checkin-normal}")
    private int pointsPerCheckin;
    
    @Value("${app.gamification.points.checkin-comeback}")
    private int pointsPerComeback;

    // ... [Hàm processCheckinGamification giữ nguyên, không cần CacheEvict ở đây vì bên CheckinService đã làm rồi] ...
    @Override
    @Async("taskExecutor")
    @Transactional
    public void processCheckinGamification(Checkin checkin) {
        log.info("Processing gamification for checkin {}", checkin.getId());

        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(checkin.getJourney().getId(), checkin.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        LocalDate today = LocalDate.now();
        LocalDate lastCheckin = participant.getLastCheckinAt();
        
        int currentStreak = participant.getCurrentStreak();
        boolean isFirstCheckinToday = false;

        if (lastCheckin != null) {
            if (lastCheckin.isEqual(today)) {
                log.info("User already checked in today.");
            } else {
                isFirstCheckinToday = true;
                if (lastCheckin.isEqual(today.minusDays(1))) {
                    currentStreak++;
                } else {
                    currentStreak = 1; 
                }
                participant.setCurrentStreak(currentStreak);
                participant.setLastCheckinAt(today);
                participantRepository.save(participant);
            }
        } else {
            isFirstCheckinToday = true;
            currentStreak = 1;
            participant.setCurrentStreak(currentStreak);
            participant.setLastCheckinAt(today);
            participantRepository.save(participant);
        }

        if (isFirstCheckinToday && (checkin.getStatus() == CheckinStatus.NORMAL || checkin.getStatus() == CheckinStatus.COMEBACK)) {
            long pointsEarned = (checkin.getStatus() == CheckinStatus.COMEBACK) ? pointsPerComeback : pointsPerCheckin; 
            awardPoints(checkin.getUser(), (int) pointsEarned, "Check-in: " + checkin.getJourney().getName());
        }

        if (checkin.getStatus() != CheckinStatus.REST) {
            checkAndAwardBadges(checkin, currentStreak);
        }
    }

    @Override
    @Transactional
    public void awardPoints(User user, int amount, String reason) {
        user.setPoints(user.getPoints() + amount);
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) amount)
                .balanceAfter(user.getPoints())
                .reason(reason)
                .source(PointSource.CHECKIN)
                .build();
        pointHistoryRepository.save(history);
    }

    private void checkAndAwardBadges(Checkin checkin, int currentStreak) {
        Set<Long> ownedBadgeIds = userBadgeRepository.findBadgeIdsByUserId(checkin.getUser().getId());
        List<Badge> streakBadges = badgeRepository.findByConditionType(BadgeConditionType.STREAK);
        
        for (Badge badge : streakBadges) {
            if (currentStreak >= badge.getConditionValue() && !ownedBadgeIds.contains(badge.getId())) {
                awardBadge(checkin.getUser(), badge, checkin.getJourney().getId());
                ownedBadgeIds.add(badge.getId());
            }
        }

        if (checkin.getStatus() == CheckinStatus.COMEBACK) {
            List<Badge> comebackBadges = badgeRepository.findByConditionType(BadgeConditionType.COMEBACK);
            for (Badge badge : comebackBadges) {
                if (!ownedBadgeIds.contains(badge.getId())) {
                    awardBadge(checkin.getUser(), badge, checkin.getJourney().getId());
                }
            }
        }
    }

    private void awardBadge(User user, Badge badge, UUID journeyId) {
        UserBadge userBadge = UserBadge.builder()
                .user(user)
                .badge(badge)
                .journeyId(journeyId)
                .earnedAt(LocalDateTime.now())
                .build();
        userBadgeRepository.save(userBadge);
        
        notificationService.sendAndSaveNotification(
                user.getId(),
                null, 
                NotificationType.SYSTEM,
                "Huy hiệu mới! 🏆",
                "Chúc mừng! Bạn đã đạt huy hiệu [" + badge.getName() + "]",
                badge.getId().toString(),
                badge.getIconUrl()
        );
        log.info("Awarded badge [{}] to user [{}]", badge.getName(), user.getId());
    }
    
    @Override
    @Transactional
    public boolean buyFreezeStreakItem(User user) {
        if (user.getPoints() < freezeItemCost) {
            throw new BadRequestException("Bạn không đủ điểm! Cần " + freezeItemCost + " điểm.");
        }
        user.setPoints(user.getPoints() - freezeItemCost);
        user.setFreezeStreakCount(user.getFreezeStreakCount() + 1);
        userRepository.save(user);
        
        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) -freezeItemCost)
                .balanceAfter(user.getPoints())
                .reason("Mua vé đóng băng")
                .source(PointSource.SHOP_PURCHASE)
                .build();
        pointHistoryRepository.save(history);
        
        return true;
    }
    
    @Override
    @Transactional
    // --- THÊM DÒNG NÀY: Xóa cache khi reset streak ---
    @CacheEvict(value = "journey_widget", key = "#journeyId + '-' + #userId")
    // -----------------------------------------------
    public void refreshUserStreak(UUID journeyId, Long userId) {
        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(journeyId, userId)
                .orElse(null);

        if (participant == null || participant.getLastCheckinAt() == null) return;

        LocalDate today = LocalDate.now();
        LocalDate lastCheckin = participant.getLastCheckinAt();

        if (lastCheckin.isBefore(today.minusDays(1))) {
            if (participant.getCurrentStreak() > 0) {
                log.info("Lazy Reset Streak for User {} in Journey {}", userId, journeyId);
                participant.setCurrentStreak(0);
                participantRepository.save(participant);
            }
        }
    }

    @Override
    public List<BadgeResponse> getUserBadges(User user) {
        return userBadgeRepository.findByUserIdOrderByEarnedAtDesc(user.getId())
                .stream()
                .map(gamificationMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PointHistoryResponse> getPointHistory(User user) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(gamificationMapper::toPointHistoryResponse)
                .collect(Collectors.toList());
    }
}