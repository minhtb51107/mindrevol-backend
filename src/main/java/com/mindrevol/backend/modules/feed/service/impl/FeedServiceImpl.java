package com.mindrevol.backend.modules.feed.service.impl;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;

    private static final String FEED_KEY_PREFIX = "user:feed:";
    private static final long FEED_TTL_DAYS = 7; // Feed tồn tại 7 ngày trong cache

    @Override
    public void pushToFeed(Long userId, Long checkinId) {
        String key = FEED_KEY_PREFIX + userId;
        
        // Push ID vào đầu danh sách (LPUSH)
        redisTemplate.opsForList().leftPush(key, checkinId.toString());
        
        // Cắt bớt nếu dài quá (giữ 200 bài mới nhất)
        redisTemplate.opsForList().trim(key, 0, 199);
        
        // Gia hạn thời gian sống
        redisTemplate.expire(key, FEED_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public List<CheckinResponse> getNewsFeed(Long userId, int offset, int limit) {
        String key = FEED_KEY_PREFIX + userId;
        
        // Lấy danh sách ID từ Redis
        List<String> checkinIdsStr = redisTemplate.opsForList().range(key, offset, offset + limit - 1);
        
        if (checkinIdsStr == null || checkinIdsStr.isEmpty()) {
            return new ArrayList<>();
        }

        // Convert String ID -> Long ID
        List<Long> checkinIds = checkinIdsStr.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // Query Database để lấy chi tiết bài viết (WHERE id IN (...))
        List<Checkin> checkins = checkinRepository.findAllById(checkinIds);

        // Map sang DTO và sắp xếp lại theo đúng thứ tự ID trong Redis (vì SQL 'IN' không bảo đảm thứ tự)
        // Cách nhanh: Map ID -> Object
        var checkinMap = checkins.stream()
                .collect(Collectors.toMap(Checkin::getId, c -> c));

        List<CheckinResponse> result = new ArrayList<>();
        for (Long id : checkinIds) {
            Checkin c = checkinMap.get(id);
            if (c != null) {
                result.add(checkinMapper.toResponse(c));
            }
        }
        
        return result;
    }

    @Override
    public void removeFromFeed(Long userId, Long checkinId) {
        String key = FEED_KEY_PREFIX + userId;
        // Xóa ID khỏi list (count = 0 nghĩa là xóa tất cả các phần tử có giá trị này)
        redisTemplate.opsForList().remove(key, 0, checkinId.toString());
    }
}