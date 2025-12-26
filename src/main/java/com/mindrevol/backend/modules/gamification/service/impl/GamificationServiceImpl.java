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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import java.util.Map;
import java.util.function.Function;

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

    private final JourneyParticipantRepository participantRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GamificationMapper gamificationMapper;

    @Value("${app.gamification.points.item-freeze-cost}")
    private int freezeItemCost;
    
    // Giá vé sửa chuỗi (thường đắt gấp đôi vé đóng băng)
    @Value("${app.gamification.points.item-repair-cost:1000}") 
    private int repairItemCost;

    @Value("${app.gamification.points.checkin-normal}")
    private int pointsPerCheckin;
    
    @Value("${app.gamification.points.checkin-comeback}")
    private int pointsPerComeback;

    @Override
    @Async("taskExecutor")
    @Transactional
    public void processCheckinGamification(Checkin checkin) {
        log.info("Processing gamification for checkin {}", checkin.getId());

        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(checkin.getJourney().getId(), checkin.getUser().getId())
                .orElse(null);
        
        if (participant == null) return;

        if (checkin.getStatus() == CheckinStatus.NORMAL || checkin.getStatus() == CheckinStatus.COMEBACK) {
            long pointsEarned = (checkin.getStatus() == CheckinStatus.COMEBACK) ? pointsPerComeback : pointsPerCheckin;
            awardPoints(checkin.getUser(), (int) pointsEarned, "Check-in: " + checkin.getJourney().getName());
        }

        if (checkin.getStatus() != CheckinStatus.REST) {
            checkAndAwardBadges(checkin, participant.getCurrentStreak());
        }
    }

    @Override
    @Transactional
    public void awardPoints(User user, int amount, String reason) {
        // [CẬP NHẬT] Sử dụng incrementPoints để tránh race condition
        userRepository.incrementPoints(user.getId(), amount);
        
        // Fetch lại user hoặc tính toán số dư mới (tạm tính để ghi log)
        // Lưu ý: Nếu muốn chính xác tuyệt đối trong log khi concurrency cao, cần fetch lại user.
        // Ở đây ta cộng tạm vào object user hiện tại để ghi log (chấp nhận sai số hiển thị nhỏ trong log nếu có đua)
        long newBalance = user.getPoints() + amount;

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) amount)
                .balanceAfter(newBalance)
                .reason(reason)
                .source(PointSource.CHECKIN)
                .build();
        pointHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void revokeGamification(Checkin checkin) {
        log.info("Revoking gamification for checkin {}", checkin.getId());
        User user = checkin.getUser();
        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(checkin.getJourney().getId(), user.getId())
                .orElse(null);

        if (participant == null) return;

        // 1. Trừ điểm (Phạt)
        int pointsRevoked = (checkin.getStatus() == CheckinStatus.COMEBACK) ? pointsPerComeback : pointsPerCheckin;
        
        // [CẬP NHẬT] Dùng incrementPoints với số âm để trừ, cho phép âm điểm khi bị phạt
        userRepository.incrementPoints(user.getId(), -pointsRevoked);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) -pointsRevoked)
                .balanceAfter(user.getPoints() - pointsRevoked)
                .reason("Bị gỡ bài check-in vi phạm (ID: " + checkin.getId() + ")")
                .source(PointSource.ADMIN_ADJUST)
                .build();
        pointHistoryRepository.save(history);

        // 2. Rollback Streak
        if (checkin.getStatus() != CheckinStatus.REST && checkin.getStatus() != CheckinStatus.REJECTED) {
            if (participant.getCurrentStreak() > 0) {
                participant.setCurrentStreak(participant.getCurrentStreak() - 1);
                
                if (participant.getLastCheckinAt() != null) {
                   participant.setLastCheckinAt(participant.getLastCheckinAt().minusDays(1));
                }
                participantRepository.save(participant);
            }
        }
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
    }
    
    @Override
    @Transactional
    public boolean buyFreezeStreakItem(User user) {
        // [CẬP NHẬT] Atomic Update - Trừ điểm an toàn
        // Hàm decrementPoints trả về số dòng update được (1 nếu thành công, 0 nếu không đủ điều kiện points >= cost)
        int rowsUpdated = userRepository.decrementPoints(user.getId(), freezeItemCost);
        
        if (rowsUpdated == 0) {
            throw new BadRequestException("Giao dịch thất bại! Bạn không đủ điểm (Cần " + freezeItemCost + " điểm) hoặc tài khoản không tồn tại.");
        }

        // Nếu trừ tiền thành công mới cộng vật phẩm
        user.setFreezeStreakCount(user.getFreezeStreakCount() + 1);
        userRepository.save(user); // Lưu lại số lượng item mới
        
        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) -freezeItemCost)
                .balanceAfter(user.getPoints() - freezeItemCost) // Balance tương đối
                .reason("Mua vé đóng băng")
                .source(PointSource.SHOP_PURCHASE)
                .build();
        pointHistoryRepository.save(history);
        
        return true;
    }

    @Override
    @Transactional
    public void repairStreak(UUID journeyId, User user) {
        JourneyParticipant participant = participantRepository.findByJourneyIdAndUserId(journeyId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không tham gia hành trình này"));

        // 1. Kiểm tra có chuỗi nào để cứu không
        if (participant.getSavedStreak() == null || participant.getSavedStreak() <= 0) {
            throw new BadRequestException("Bạn không có chuỗi nào bị mất gần đây để khôi phục.");
        }
        
        // 2. [CẬP NHẬT] Atomic Update - Trừ điểm an toàn
        int rowsUpdated = userRepository.decrementPoints(user.getId(), repairItemCost);
        if (rowsUpdated == 0) {
             throw new BadRequestException("Bạn không đủ điểm để sửa chuỗi! Cần " + repairItemCost + " điểm.");
        }

        // --- Logic sửa chuỗi an toàn & đúng múi giờ ---
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        LocalDate todayUser;
        try {
            todayUser = LocalDate.now(java.time.ZoneId.of(tz));
        } catch (Exception e) {
            todayUser = LocalDate.now();
        }

        boolean hasCheckedInToday = participant.getLastCheckinAt() != null && 
                                    participant.getLastCheckinAt().isEqual(todayUser);

        if (hasCheckedInToday) {
            // Đã check-in hôm nay: Cộng dồn chuỗi cũ vào chuỗi hiện tại
            participant.setCurrentStreak(participant.getSavedStreak() + participant.getCurrentStreak());
        } else {
            // Chưa check-in hôm nay: Khôi phục chuỗi và giả lập check-in hôm qua
            participant.setCurrentStreak(participant.getSavedStreak());
            participant.setLastCheckinAt(todayUser.minusDays(1));
        }
        
        participant.setSavedStreak(0); // Xóa backup
        participantRepository.save(participant);

        // 4. Ghi log
        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) -repairItemCost)
                .balanceAfter(user.getPoints() - repairItemCost)
                .reason("Sửa chuỗi (Repair Streak)")
                .source(PointSource.SHOP_PURCHASE)
                .build();
        pointHistoryRepository.save(history);
        
        log.info("User {} repaired streak for journey {}", user.getId(), journeyId);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "journey_widget", key = "#journeyId + '-' + #userId")
    public void refreshUserStreak(UUID journeyId, Long userId) {
        JourneyParticipant participant = participantRepository
                .findByJourneyIdAndUserId(journeyId, userId)
                .orElse(null);

        if (participant == null || participant.getLastCheckinAt() == null) return;

        LocalDate today = LocalDate.now(); 
        LocalDate lastCheckin = participant.getLastCheckinAt();

        if (lastCheckin.isBefore(today.minusDays(1))) {
            if (participant.getCurrentStreak() > 0) {
                log.info("Lazy Reset Streak check for User {} in Journey {}", userId, journeyId);
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
    
    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getAllBadgesWithStatus(User user) {
        List<Badge> allBadges = badgeRepository.findAll();
        List<UserBadge> userBadges = userBadgeRepository.findByUserId(user.getId());

        Map<Long, UserBadge> userBadgeMap = userBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), Function.identity()));

        return allBadges.stream()
                .map(badge -> {
                    UserBadge owned = userBadgeMap.get(badge.getId());
                    return BadgeResponse.builder()
                            .id(badge.getId())
                            .name(badge.getName())
                            .description(badge.getDescription())
                            .iconUrl(badge.getIconUrl())
                            .conditionType(badge.getConditionType() != null ? badge.getConditionType().name() : "")
                            .requiredValue(badge.getConditionValue()) 
                            .isOwned(owned != null)
                            .obtainedAt(owned != null ? owned.getEarnedAt() : null) 
                            .build();
                })
                .collect(Collectors.toList());
    }
}