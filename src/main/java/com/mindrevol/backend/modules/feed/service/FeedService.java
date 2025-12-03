package com.mindrevol.backend.modules.feed.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;

    private static final String FEED_KEY_PREFIX = "user:feed:";
    private static final long FEED_TTL_DAYS = 30; // Feed lưu trong Redis 30 ngày

    /**
     * Đẩy bài viết vào Feed của một người dùng cụ thể.
     */
    public void pushToFeed(Long userId, UUID checkinId) {
        String key = FEED_KEY_PREFIX + userId;
        // Đẩy vào đầu danh sách (LPUSH)
        redisTemplate.opsForList().leftPush(key, checkinId.toString());
        
        // Cắt bớt nếu quá dài (ví dụ chỉ giữ 500 bài mới nhất để tiết kiệm RAM)
        redisTemplate.opsForList().trim(key, 0, 500);
        
        // Gia hạn thời gian sống
        redisTemplate.expire(key, FEED_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * Lấy Feed từ Redis (nhanh hơn DB query phức tạp)
     */
    public List<CheckinResponse> getFeed(Long userId, int page, int size) {
        String key = FEED_KEY_PREFIX + userId;
        
        long start = (long) page * size;
        long end = start + size - 1;

        // 1. Lấy danh sách ID từ Redis
        List<Object> idObjects = redisTemplate.opsForList().range(key, start, end);
        
        if (idObjects == null || idObjects.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> checkinIds = idObjects.stream()
                .map(obj -> UUID.fromString((String) obj))
                .collect(Collectors.toList());

        // 2. Fetch data chi tiết từ DB (Dùng findAllById rất nhanh)
        // findAllById không bảo đảm thứ tự trả về giống list ID đầu vào
        List<Checkin> checkins = checkinRepository.findAllById(checkinIds);

        // 3. Sắp xếp lại theo thứ tự của Redis (Mới nhất trước)
        List<Checkin> sortedCheckins = new ArrayList<>();
        for (UUID id : checkinIds) {
            checkins.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .ifPresent(sortedCheckins::add);
        }

        return sortedCheckins.stream()
                .map(checkinMapper::toResponse)
                .collect(Collectors.toList());
    }
}