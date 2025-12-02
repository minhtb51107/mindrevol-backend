package com.mindrevol.backend.modules.gamification.service;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse; // Import
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface GamificationService {
    void processCheckinGamification(Checkin checkin);
    
    List<BadgeResponse> getUserBadges(User user);
    
    // Thêm hàm này
    List<PointHistoryResponse> getPointHistory(User user); 
    
    void refreshUserStreak(UUID journeyId, Long userId);
    
    boolean buyFreezeStreakItem(User user);
    
    // Update signature: thêm reason
    void awardPoints(User user, int amount, String reason); 
}