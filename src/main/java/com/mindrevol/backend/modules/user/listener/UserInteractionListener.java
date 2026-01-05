package com.mindrevol.backend.modules.user.listener;

import com.mindrevol.backend.modules.auth.repository.UserActivationTokenRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserInteractionListener {

    private final UserRepository userRepository;
    // Đã xóa GamificationService

    @Async
    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            
            // Chỉ update last active, không cộng điểm nữa
            userRepository.findByEmail(email).ifPresent(user -> {
                // Logic update last_active_at có thể thêm vào đây nếu Entity User có field đó
                log.info("User {} just logged in.", email);
            });
        }
    }
}