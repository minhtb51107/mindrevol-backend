package com.mindrevol.backend.modules.auth.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.utils.JwtUtil;
import com.mindrevol.backend.modules.auth.dto.RedisUserSession;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.service.SessionService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions_map:";

    @Override
    public JwtResponse createTokenAndSession(User user, HttpServletRequest request) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        // Định danh duy nhất cho cái "iPad" hoặc "Điện thoại" này
        String sessionId = UUID.randomUUID().toString(); 
        
        RedisUserSession redisSession = RedisUserSession.builder()
                .id(sessionId)
                .email(user.getEmail())
                .refreshToken(refreshToken)
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .expiredAt(System.currentTimeMillis() + refreshTokenExpirationMs)
                .build();
                
        // TỦ 1: Cất chi tiết Session (Key = Token)
        String redisKey = SESSION_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(redisKey, redisSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        
        // TỦ 2 (Mục lục Hash): user_sessions:{email} -> Cất cặp [sessionId : refreshToken]
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        redisTemplate.opsForHash().put(userSessionsKey, sessionId, refreshToken);
        redisTemplate.expire(userSessionsKey, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        
        log.info("Saved session for user: {} with sessionId: {}", user.getEmail(), sessionId);
        return JwtResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(redisKey);
        
        if (session == null) throw new BadRequestException("Invalid Refresh Token");
        
        User user = userRepository.findByEmail(session.getEmail()).orElseThrow();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getEmail();
        
        // Quan trọng: Giữ lại cái ID định danh của thiết bị cũ
        String sessionId = session.getId(); 

        // Xóa Token cũ ở Tủ 1
        redisTemplate.delete(redisKey);
        
        RedisUserSession newSession = RedisUserSession.builder()
                .id(sessionId)
                .email(user.getEmail())
                .refreshToken(newRefreshToken)
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .expiredAt(System.currentTimeMillis() + refreshTokenExpirationMs)
                .build();
                
        // Lưu Token mới vào Tủ 1
        String newRedisKey = SESSION_PREFIX + newRefreshToken;
        redisTemplate.opsForValue().set(newRedisKey, newSession, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        
        // Cập nhật lại Tủ 2: Mở đúng ngăn tủ sessionId, đè cái chìa khóa mới vào (Vẫn là O(1))
        redisTemplate.opsForHash().put(userSessionsKey, sessionId, newRefreshToken);
        redisTemplate.expire(userSessionsKey, refreshTokenExpirationMs, TimeUnit.MILLISECONDS);
        
        return JwtResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
    }

    @Override
    public void logout(String refreshToken) {
        String redisKey = SESSION_PREFIX + refreshToken;
        RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(redisKey);
        if (session != null) {
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getEmail();
            // Xóa ở Tủ 2
            redisTemplate.opsForHash().delete(userSessionsKey, session.getId());
            // Xóa ở Tủ 1
            redisTemplate.delete(redisKey);
        }
    }

    @Override
    public List<UserSessionResponse> getAllSessions(String userEmail, String currentTokenRaw) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userEmail;
        
        // Lôi nguyên cuốn sổ mục lục ra
        Map<Object, Object> sessionMap = redisTemplate.opsForHash().entries(userSessionsKey);
        List<UserSessionResponse> responses = new ArrayList<>();
        
        for (Map.Entry<Object, Object> entry : sessionMap.entrySet()) {
            String sessionId = (String) entry.getKey();
            String token = (String) entry.getValue();
            
            String sessionKey = SESSION_PREFIX + token;
            RedisUserSession session = (RedisUserSession) redisTemplate.opsForValue().get(sessionKey);
            
            if (session != null) {
                responses.add(UserSessionResponse.builder()
                        .id(session.getId())
                        .ipAddress(session.getIpAddress())
                        .userAgent(session.getUserAgent())
                        .expiresAt(UserSessionResponse.mapToLocalDateTime(session.getExpiredAt()))
                        .isCurrent(false) 
                        .build());
            } else {
                // Nếu Token ở Tủ 1 đã tự bốc hơi vì hết hạn, ta tiện tay lau luôn dòng ghi chú ở Tủ 2
                redisTemplate.opsForHash().delete(userSessionsKey, sessionId);
            }
        }
        return responses;
    }

    @Override
    public void revokeSession(String sessionId, String userEmail) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userEmail;
        
        // 🚀 BƯỚC 1: Tra Tủ Mục Lục bằng sessionId để lấy refreshToken (Độ phức tạp: O(1))
        String token = (String) redisTemplate.opsForHash().get(userSessionsKey, sessionId);
        
        if (token != null) {
            String sessionKey = SESSION_PREFIX + token;
            
            // 🚀 BƯỚC 2: Xóa Token ở Tủ 1 (Độ phức tạp: O(1))
            redisTemplate.delete(sessionKey);
            
            // 🚀 BƯỚC 3: Xóa dòng mục lục ở Tủ 2 (Độ phức tạp: O(1))
            redisTemplate.opsForHash().delete(userSessionsKey, sessionId);
            return;
        }
        
        throw new ResourceNotFoundException("Session not found");
    }
}