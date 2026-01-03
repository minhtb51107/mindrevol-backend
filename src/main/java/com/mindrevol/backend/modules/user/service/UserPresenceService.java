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
    private static final long ONLINE_TIMEOUT_MINUTES = 5; 

    public void connect(String userId) { // [UUID]
        String key = ONLINE_USERS_KEY + userId;
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
        redisTemplate.expire(key, ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        log.debug("User {} is ONLINE", userId);
    }

    public void disconnect(String userId) { // [UUID]
        String key = ONLINE_USERS_KEY + userId;
        redisTemplate.delete(key);
        log.debug("User {} is OFFLINE", userId);
    }

    public boolean isUserOnline(String userId) { // [UUID]
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USERS_KEY + userId));
    }
    
    public void heartbeat(String userId) { // [UUID]
        String key = ONLINE_USERS_KEY + userId;
        redisTemplate.expire(key, ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }
}