package com.mindrevol.backend.modules.gamification.service.impl;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.entity.*;
import com.mindrevol.backend.modules.gamification.mapper.GamificationMapper;
import com.mindrevol.backend.modules.gamification.repository.BadgeRepository;
import com.mindrevol.backend.modules.gamification.repository.PointHistoryRepository;
import com.mindrevol.backend.modules.gamification.repository.UserBadgeRepository;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GamificationMapper gamificationMapper;

    private static final int POINT_PER_CHECKIN = 10;

    @Async
    @Override
    @Transactional
    public void awardPointsForCheckin(Checkin checkin) {
        User user = checkin.getUser();
        user.setPoints(user.getPoints() + POINT_PER_CHECKIN);
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount((long) POINT_PER_CHECKIN)
                .balanceAfter(user.getPoints())
                .reason("Check-in: " + checkin.getJourney().getName())
                .source(PointSource.CHECKIN)
                .build();
        pointHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void revokeGamification(Checkin checkin) {
        User user = checkin.getUser();
        long pointsToRevoke = POINT_PER_CHECKIN;

        if (user.getPoints() >= pointsToRevoke) {
            user.setPoints(user.getPoints() - pointsToRevoke);
        } else {
            user.setPoints(0L);
        }
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(-pointsToRevoke)
                .balanceAfter(user.getPoints())
                .reason("Bị trừ điểm do bài viết bị gỡ: " + checkin.getId())
                .source(PointSource.ADMIN_ADJUST) 
                .build();
        pointHistoryRepository.save(history);
    }

    // --- [THÊM MỚI] Hàm cộng điểm cho Payment ---
    @Override
    @Transactional
    public void awardPoints(User user, long amount, String reason) {
        if (amount <= 0) return; // Không cộng số âm hoặc 0 ở hàm này

        user.setPoints(user.getPoints() + amount);
        userRepository.save(user);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(amount)
                .balanceAfter(user.getPoints())
                .reason(reason)
                .source(PointSource.TOPUP) // Đánh dấu là Nạp tiền
                .build();
        
        pointHistoryRepository.save(history);
        log.info("Awarded {} points to user {} via TOPUP", amount, user.getId());
    }
    // ---------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getAllBadgesWithStatus(User user) {
        List<Badge> allBadges = badgeRepository.findAll();
        Set<Long> ownedBadgeIds = userBadgeRepository.findBadgeIdsByUserId(user.getId());

        return allBadges.stream().map(badge -> {
            BadgeResponse response = gamificationMapper.toBadgeResponse(badge);
            response.setOwned(ownedBadgeIds.contains(badge.getId()));
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getPointHistory(User user) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(gamificationMapper::toPointHistoryResponse)
                .collect(Collectors.toList());
    }
}