package com.mindrevol.backend.modules.feed.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import org.springframework.data.domain.Pageable; // Hoặc Page/List tùy code cũ của bạn

import java.util.List;

public interface FeedService {
    
    // [FIX] Đổi UUID -> Long
    void pushToFeed(Long userId, Long checkinId);

    // Hàm lấy feed (tùy code cũ của bạn, nhưng cũng nên đảm bảo trả về CheckinResponse)
    List<CheckinResponse> getNewsFeed(Long userId, int offset, int limit);
    
    // Hàm xóa khỏi feed (nếu có)
    void removeFromFeed(Long userId, Long checkinId);
}