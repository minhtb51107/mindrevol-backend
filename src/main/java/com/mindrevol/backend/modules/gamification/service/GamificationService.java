package com.mindrevol.backend.modules.gamification.service;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface GamificationService {
    /**
     * Xử lý logic game hóa sau khi check-in: Tính streak, trao badge.
     * @param checkin Bài check-in vừa tạo
     */
    void processCheckinGamification(Checkin checkin);

    /**
     * Lấy danh sách huy hiệu của user
     */
    List<BadgeResponse> getUserBadges(User user);
    
    void refreshUserStreak(UUID journeyId, Long userId);

	boolean buyFreezeStreakItem(User user);

	void awardPoints(User user, int amount);
}