package com.mindrevol.backend.config;

import com.mindrevol.backend.modules.user.entity.User;
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
    // [SỬA 1] Đổi AuditorAware<Long> thành AuditorAware<String>
    public AuditorAware<String> auditorProvider() { 
        return new AuditorAwareImpl();
    }

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    // [SỬA 2] Implement AuditorAware<String> thay vì AuditorAware<Long>
    public static class AuditorAwareImpl implements AuditorAware<String> {
        
        @Override
        // [SỬA 3] Trả về Optional<String>
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 1. Kiểm tra nếu chưa đăng nhập hoặc là anonymousUser
            if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken ||
                "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }

            // 2. Lấy Principal
            Object principal = authentication.getPrincipal();

            // 3. Kiểm tra và ép kiểu sang Entity User
            if (principal instanceof User) {
                // userId bây giờ là String (UUID), nên trả về đúng kiểu String là hợp lệ
                return Optional.ofNullable(((User) principal).getId());
            }

            return Optional.empty();
        }
    }
}