package com.mindrevol.backend.modules.feed.service.impl;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.feed.service.FeedService;
import com.mindrevol.backend.modules.user.repository.UserBlockRepository; // [THÊM]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest; // [THÊM]
import org.springframework.data.domain.Pageable; // [THÊM]
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime; // [THÊM]
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // [THÊM] Để log lỗi nếu cần
public class FeedServiceImpl implements FeedService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final UserBlockRepository userBlockRepository; // [THÊM] Inject thêm cái này để lọc user bị chặn

    private static final String FEED_KEY_PREFIX = "user:feed:";
    private static final long FEED_TTL_DAYS = 7; 

    @Override
    public void pushToFeed(String userId, String checkinId) { 
        String key = FEED_KEY_PREFIX + userId;
        try {
            // Push String ID vào list
            redisTemplate.opsForList().leftPush(key, checkinId);
            redisTemplate.opsForList().trim(key, 0, 199);
            redisTemplate.expire(key, FEED_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Lỗi khi push Redis feed cho user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public List<CheckinResponse> getNewsFeed(String userId, int offset, int limit) {
        String key = FEED_KEY_PREFIX + userId;
        List<CheckinResponse> result = new ArrayList<>();
        
        // 1. Cố gắng lấy từ Redis trước
        List<String> checkinIds = null;
        try {
            checkinIds = redisTemplate.opsForList().range(key, offset, offset + limit - 1);
        } catch (Exception e) {
            log.warn("Redis feed error, falling back to DB: {}", e.getMessage());
        }

        // 2. [LOGIC MỚI] Nếu Redis có dữ liệu -> Query DB theo ID
        if (checkinIds != null && !checkinIds.isEmpty()) {
            List<Checkin> checkins = checkinRepository.findAllById(checkinIds);
            Map<String, Checkin> checkinMap = checkins.stream()
                    .collect(Collectors.toMap(Checkin::getId, c -> c));

            for (String id : checkinIds) {
                Checkin c = checkinMap.get(id);
                if (c != null) {
                    result.add(checkinMapper.toResponse(c));
                }
            }
            // Nếu tìm được dữ liệu từ Redis, trả về luôn
            if (!result.isEmpty()) {
                return result;
            }
        }

        // 3. [FALLBACK] Nếu Redis rỗng (hoặc query ID không ra), lấy trực tiếp từ DB
        // Đây là phần sửa quan trọng để bạn thấy bài đăng khi mới chạy app
        log.info("Feed Cache miss or empty for user {}. Fetching from Database...", userId);
        return getFallbackFeedFromDb(userId, offset, limit);
    }

    // [THÊM] Hàm hỗ trợ lấy từ DB giống như logic bên CheckinService
    private List<CheckinResponse> getFallbackFeedFromDb(String userId, int offset, int limit) {
        // Lấy danh sách user bị chặn để lọc
        Set<String> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        if (blockedIds == null) blockedIds = new HashSet<>();
        blockedIds.removeIf(Objects::isNull);
        blockedIds.add("00000000-0000-0000-0000-000000000000"); // Tránh lỗi SQL IN empty

        // Tính toán phân trang
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        
        // Dùng thời gian tương lai để lấy tất cả bài mới nhất
        LocalDateTime cursor = LocalDateTime.now().plusDays(1); 

        // Gọi Repository (đã có sẵn hàm này trong CheckinRepository bạn upload)
        List<Checkin> dbCheckins = checkinRepository.findUnifiedFeed(
            userId, 
            cursor, 
            blockedIds, 
            pageable
        );

        return dbCheckins.stream()
                .map(checkinMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFromFeed(String userId, String checkinId) {
        String key = FEED_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForList().remove(key, 0, checkinId);
        } catch (Exception e) {
            log.error("Error removing from feed: {}", e.getMessage());
        }
    }
}