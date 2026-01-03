package com.mindrevol.backend.modules.gamification.service.impl;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
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
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final JourneyParticipantRepository participantRepository;
    private final GamificationMapper gamificationMapper;

    private static final int POINTS_PER_CHECKIN = 10;
    private static final int POINTS_PER_STREAK_BONUS = 50;

    @Override
    @Transactional
    public void awardPoints(String userId, int amount, PointSource source, String description, String refId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.incrementPoints(userId, amount);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(amount)
                .source(source)
                .description(description)
                .referenceId(refId)
                .build(); // BaseEntity tự lo createdAt
        
        pointHistoryRepository.save(history);
        log.info("Awarded {} points to User {}", amount, userId);
    }

    @Override
    @Transactional
    public void processCheckinGamification(Checkin checkin) {
        User user = checkin.getUser();
        String userId = user.getId();
        
        // 1. Cộng điểm cơ bản
        awardPoints(userId, POINTS_PER_CHECKIN, PointSource.CHECKIN, "Check-in daily", checkin.getId());

        // 2. Kiểm tra Streak
        // [FIX] Repository đã trả về object hoặc String ID đúng
        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(checkin.getJourney().getId(), userId)
                .orElse(null);

        if (participant != null) {
            checkStreakBadges(user, participant.getCurrentStreak());
            
            // Thưởng thêm nếu đạt mốc streak (ví dụ mỗi 7 ngày)
            if (participant.getCurrentStreak() > 0 && participant.getCurrentStreak() % 7 == 0) {
                awardPoints(userId, POINTS_PER_STREAK_BONUS, PointSource.STREAK, 
                        "Streak " + participant.getCurrentStreak() + " days bonus!", null);
            }
        }
        
        // 3. Kiểm tra Tổng số bài checkin (Milestone)
        checkTotalCheckinBadges(user, participant != null ? participant.getTotalCheckins() : 0);
    }

    @Override
    @Transactional
    public void revokeGamification(Checkin checkin) {
        // [FIX] checkin.getId() là String
        String userId = checkin.getUser().getId();
        
        // Trừ điểm đã cộng
        // Logic đơn giản: Trừ đúng số điểm checkin cơ bản
        // (Không rollback streak bonus phức tạp để tránh bug âm điểm quá mức)
        userRepository.decrementPoints(userId, POINTS_PER_CHECKIN);
        
        // Ghi log trừ điểm (số âm)
        PointHistory history = PointHistory.builder()
                .user(checkin.getUser())
                .amount(-POINTS_PER_CHECKIN)
                .source(PointSource.REVOKED)
                .description("Check-in revoked/deleted")
                .referenceId(checkin.getId())
                .build();
        pointHistoryRepository.save(history);
    }

    private void checkStreakBadges(User user, int currentStreak) {
        List<Badge> streakBadges = badgeRepository.findByConditionType(BadgeConditionType.STREAK);
        
        for (Badge badge : streakBadges) {
            if (currentStreak >= badge.getConditionValue()) {
                grantBadgeIfNotExists(user, badge);
            }
        }
    }
    
    private void checkTotalCheckinBadges(User user, int total) {
        List<Badge> milestoneBadges = badgeRepository.findByConditionType(BadgeConditionType.TOTAL_CHECKINS);
        
        for (Badge badge : milestoneBadges) {
            if (total >= badge.getConditionValue()) {
                grantBadgeIfNotExists(user, badge);
            }
        }
    }

    private void grantBadgeIfNotExists(User user, Badge badge) {
        if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            UserBadge userBadge = UserBadge.builder()
                    .user(user)
                    .badge(badge)
                    .earnedAt(LocalDateTime.now())
                    .build();
            userBadgeRepository.save(userBadge);
            
            // Có thể bắn noti: Chúc mừng bạn nhận huy hiệu mới
            log.info("User {} earned badge {}", user.getId(), badge.getName());
        }
    }

    @Override
    public List<BadgeResponse> getMyBadges(String userId) {
        return userBadgeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(gamificationMapper::toUserBadgeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PointHistoryResponse> getMyPointHistory(String userId, Pageable pageable) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(gamificationMapper::toPointHistoryResponse);
    }
}