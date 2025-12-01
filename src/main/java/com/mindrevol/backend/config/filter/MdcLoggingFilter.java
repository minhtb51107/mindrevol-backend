package com.mindrevol.backend.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Chạy đầu tiên, trước cả RateLimit
public class MdcLoggingFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 1. Tạo ID hoặc lấy từ header (nếu Frontend gửi lên)
            HttpServletRequest req = (HttpServletRequest) request;
            String requestId = req.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString().substring(0, 8);
            }

            // 2. Đưa vào MDC (để Logback dùng)
            MDC.put(MDC_KEY, requestId);

            // 3. Trả lại Header cho Frontend biết để trace
            HttpServletResponse res = (HttpServletResponse) response;
            res.setHeader(REQUEST_ID_HEADER, requestId);

            chain.doFilter(request, response);
        } finally {
            // 4. Dọn dẹp (Quan trọng vì ThreadPool tái sử dụng thread)
            MDC.remove(MDC_KEY);
        }
    }
}