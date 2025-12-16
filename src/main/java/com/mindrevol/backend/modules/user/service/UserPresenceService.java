package com.mindrevol.backend.modules.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_USERS_KEY = "users:online:";
    private static final long ONLINE_TIMEOUT_MINUTES = 5; // Tự động offline nếu mất kết nối socket quá 5p

    // Đánh dấu User lên mạng
    public void connect(Long userId) {
        String key = ONLINE_USERS_KEY + userId;
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
        redisTemplate.expire(key, ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        log.debug("User {} is ONLINE", userId);
    }

    // Đánh dấu User thoát
    public void disconnect(Long userId) {
        String key = ONLINE_USERS_KEY + userId;
        redisTemplate.delete(key);
        // Ở đây có thể cập nhật lastActiveAt vào Database nếu muốn lưu lịch sử
        log.debug("User {} is OFFLINE", userId);
    }

    // Kiểm tra User có đang online không
    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USERS_KEY + userId));
    }
    
    // Gia hạn thời gian online (Heartbeat)
    public void heartbeat(Long userId) {
        String key = ONLINE_USERS_KEY + userId;
        redisTemplate.expire(key, ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }
}