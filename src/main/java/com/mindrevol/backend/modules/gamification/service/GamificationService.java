package com.mindrevol.backend.modules.gamification.service;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.entity.PointSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GamificationService {
    // [UUID] String userId
    void awardPoints(String userId, int amount, PointSource source, String description, String refId);
    
    // Xử lý logic checkin để cộng điểm và kiểm tra badge
    void processCheckinGamification(Checkin checkin);
    
    // Thu hồi điểm khi xóa bài
    void revokeGamification(Checkin checkin);

    List<BadgeResponse> getMyBadges(String userId);
    
    Page<PointHistoryResponse> getMyPointHistory(String userId, Pageable pageable);
}