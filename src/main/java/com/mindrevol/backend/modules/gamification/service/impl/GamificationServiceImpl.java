package com.mindrevol.backend.modules.gamification.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.BadgeConditionType;
import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import com.mindrevol.backend.modules.gamification.mapper.GamificationMapper; // Import Mapper
import com.mindrevol.backend.modules.gamification.repository.BadgeRepository;
import com.mindrevol.backend.modules.gamification.repository.UserBadgeRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final JourneyParticipantRepository participantRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GamificationMapper gamificationMapper; // Inject Mapper
    private final UserRepository userRepository; 
    private static final int FREEZE_ITEM_COST = 500; // 500 điểm đổi 1 vé
    private static final int POINTS_PER_CHECKIN = 50; // Mỗi lần check-in được 50 điểm

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

        // LOGIC TÍNH STREAK
        int currentStreak = participant.getCurrentStreak();

        if (lastCheckin != null) {
            if (lastCheckin.isEqual(today)) {
                log.info("User already checked in today. Streak remains: {}", currentStreak);
            } else if (lastCheckin.isEqual(today.minusDays(1))) {
                currentStreak++;
                participant.setCurrentStreak(currentStreak);
                participant.setLastCheckinAt(today);
                participantRepository.save(participant);
            } else {
                currentStreak = 1;
                participant.setCurrentStreak(currentStreak);
                participant.setLastCheckinAt(today);
                participantRepository.save(participant);
            }
        } else {
            currentStreak = 1;
            participant.setCurrentStreak(currentStreak);
            participant.setLastCheckinAt(today);
            participantRepository.save(participant);
        }
        
     // 1. CỘNG ĐIỂM
        awardPoints(checkin.getUser(), POINTS_PER_CHECKIN);

        // 2. XỬ LÝ STREAK VỚI TRẠNG THÁI 'REST'
        if (checkin.getStatus() == CheckinStatus.REST) {
            // Nếu là ngày nghỉ:
            // - Cập nhật ngày check-in mới nhất là hôm nay (để ngày mai không bị reset về 0)
            // - KHÔNG tăng streak (giữ nguyên số streak hiện tại)
            participant.setLastCheckinAt(LocalDate.now());
            participantRepository.save(participant);
            
            log.info("User used Freeze Streak. Streak maintained at: {}", participant.getCurrentStreak());
            return; // Kết thúc, không xét badge streak
        }

        checkAndAwardBadges(checkin, currentStreak);
    }

    @Override
    @Transactional
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
                .map(gamificationMapper::toResponse) // Dùng method reference
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public boolean buyFreezeStreakItem(User user) {
        // 1. Kiểm tra đủ tiền không
        if (user.getPoints() < FREEZE_ITEM_COST) {
            throw new BadRequestException("Bạn không đủ điểm! Cần " + FREEZE_ITEM_COST + " điểm.");
        }

        // 2. Trừ tiền, cộng đồ
        user.setPoints(user.getPoints() - FREEZE_ITEM_COST);
        user.setFreezeStreakCount(user.getFreezeStreakCount() + 1);
        
        userRepository.save(user); // Nhớ inject UserRepository
        log.info("User {} bought a Freeze Streak item. Remaining points: {}", user.getId(), user.getPoints());
        return true;
    }

    @Override
    @Transactional
    public void awardPoints(User user, int amount) {
        user.setPoints(user.getPoints() + amount);
        userRepository.save(user);
    }

    private void checkAndAwardBadges(Checkin checkin, int currentStreak) {
        List<Badge> streakBadges = badgeRepository.findByConditionType(BadgeConditionType.STREAK);
        List<Badge> comebackBadges = badgeRepository.findByConditionType(BadgeConditionType.COMEBACK);

        for (Badge badge : streakBadges) {
            if (currentStreak >= badge.getConditionValue()) {
                awardBadgeIfNotExists(checkin.getUser(), badge, checkin.getJourney().getId());
            }
        }

        if (checkin.getStatus() == CheckinStatus.COMEBACK) {
            for (Badge badge : comebackBadges) {
                awardBadgeIfNotExists(checkin.getUser(), badge, checkin.getJourney().getId());
            }
        }
    }

    private void awardBadgeIfNotExists(User user, Badge badge, java.util.UUID journeyId) {
        if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            UserBadge userBadge = UserBadge.builder()
                    .user(user)
                    .badge(badge)
                    .journeyId(journeyId)
                    .earnedAt(LocalDateTime.now())
                    .build();
            userBadgeRepository.save(userBadge);
            log.info("Awarded badge [{}] to user [{}]", badge.getName(), user.getId());
        }
    }
}