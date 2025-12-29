package com.mindrevol.backend.config;

import com.mindrevol.backend.modules.user.entity.User; // Import chính xác Entity User của bạn
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() { // [QUAN TRỌNG] Phải trả về Long
        return new AuditorAwareImpl();
    }

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    public static class AuditorAwareImpl implements AuditorAware<Long> {
        @Override
        public Optional<Long> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 1. Kiểm tra nếu chưa đăng nhập hoặc là anonymousUser
            if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken ||
                "anonymousUser".equals(authentication.getPrincipal())) { // Xử lý trường hợp chuỗi "anonymousUser"
                return Optional.empty(); // Trả về null cho database (cột created_by sẽ là null)
            }

            // 2. Lấy Principal
            Object principal = authentication.getPrincipal();

            // 3. Kiểm tra và ép kiểu sang Entity User của bạn
            if (principal instanceof User) {
                Long userId = ((User) principal).getId();
                return Optional.ofNullable(userId);
            }

            return Optional.empty();
        }
    }
}