package com.mindrevol.backend.modules.gamification.service;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import java.util.List;

public interface GamificationService {
    
    void awardPointsForCheckin(Checkin checkin);
    
    void revokeGamification(Checkin checkin);

    // [THÊM MỚI] Hàm cộng điểm tổng quát (cho Payment hoặc Admin)
    void awardPoints(User user, long amount, String reason);

    List<BadgeResponse> getAllBadgesWithStatus(User user);
    List<PointHistoryResponse> getPointHistory(User user);
}