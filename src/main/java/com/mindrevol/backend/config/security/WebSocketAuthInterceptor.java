package com.mindrevol.backend.config.security;

import com.mindrevol.backend.common.utils.JwtUtil;
import com.mindrevol.backend.config.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ kiểm tra khi Client cố gắng KẾT NỐI (CONNECT)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            
            // 1. Lấy Token từ Header (nếu có) hoặc Query Param (thường dùng cho WS)
            String token = getToken(accessor);

            if (token != null && jwtUtil.validateToken(token)) {
                // 2. Trích xuất username (email) từ Token
                String username = jwtUtil.extractUsername(token);
                
                // 3. Load thông tin User từ DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 4. Tạo đối tượng Authentication
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                // 5. Gán vào Accessor -> Spring Security sẽ biết User này là ai trong suốt phiên WS
                accessor.setUser(authentication);
                
                log.info("WebSocket Authenticated User: {}", username);
            } else {
                log.warn("WebSocket Connection Rejected: Invalid or Missing Token");
                // Có thể ném Exception để từ chối kết nối, nhưng thường Spring sẽ tự ngắt nếu user null
            }
        }
        return message;
    }

    private String getToken(StompHeaderAccessor accessor) {
        // Cách 1: Lấy từ Header "Authorization" (Bearer ...)
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String bearerToken = authorization.get(0);
            if (bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }

        // Cách 2: Lấy từ Query Param (?token=...) -> Phổ biến cho thư viện JS client
        // Vì StompHeaderAccessor không hỗ trợ getQuery trực tiếp dễ dàng, ta lấy từ native headers
        // Tuy nhiên, cấu hình chuẩn thường là Client gửi header "passcode" hoặc "token" trong frame CONNECT
        List<String> tokenHeader = accessor.getNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader.get(0);
        }

        return null;
    }
}