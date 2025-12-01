package com.mindrevol.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.service.RateLimitingService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper; 

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String uri = request.getRequestURI();

        Bucket bucket;
        
        if (uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/register")) {
            bucket = rateLimitingService.resolveLoginBucket(ip);
        } else if (uri.startsWith("/api/v1/")) {
            bucket = rateLimitingService.resolveGeneralBucket(ip);
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Thử lấy 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            log.warn("Rate limit exceeded for IP: {} on URI: {}", ip, uri);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); 
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(waitForRefill));

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    429, 
                    "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau " + waitForRefill + " giây."
            );
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}