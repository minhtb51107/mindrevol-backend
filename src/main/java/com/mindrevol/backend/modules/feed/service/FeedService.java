package com.mindrevol.backend.modules.feed.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import java.util.List;

public interface FeedService {
    
    // [UUID] Đổi tất cả ID sang String
    void pushToFeed(String userId, String checkinId);

    List<CheckinResponse> getNewsFeed(String userId, int offset, int limit);
    
    void removeFromFeed(String userId, String checkinId);
}