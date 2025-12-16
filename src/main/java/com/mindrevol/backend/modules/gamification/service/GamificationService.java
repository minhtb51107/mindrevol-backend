package com.mindrevol.backend.modules.gamification.service;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface GamificationService {
    void processCheckinGamification(Checkin checkin);
    
    List<BadgeResponse> getUserBadges(User user);
    
    List<PointHistoryResponse> getPointHistory(User user); 
    
    void refreshUserStreak(UUID journeyId, Long userId);
    
    boolean buyFreezeStreakItem(User user);
    
    void awardPoints(User user, int amount, String reason); 

    // Thu hồi điểm và Streak (Dùng khi bài check-in bị report là Fake)
    void revokeGamification(Checkin checkin);

    // --- [MỚI] Sửa chuỗi (Trả tiền để khôi phục chuỗi đã mất) ---
    void repairStreak(UUID journeyId, User user);
    
 // Thêm dòng này vào interface
    List<BadgeResponse> getAllBadgesWithStatus(User user);
}