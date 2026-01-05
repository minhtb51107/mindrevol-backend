package com.mindrevol.backend.modules.feed.service;

import com.mindrevol.backend.modules.feed.dto.FeedItemResponse;
import java.util.List;

public interface FeedService {
    // Lấy feed (Cache Checkin + Live Ads)
    List<FeedItemResponse> getNewsFeed(String userId, int offset, int limit);

    void evictFeedCache(String userId);
}