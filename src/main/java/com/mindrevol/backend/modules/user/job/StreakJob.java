package com.mindrevol.backend.modules.user.job;

import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StreakJob {
    private final UserRepository userRepository;

    @Scheduled(cron = "0 1 0 * * ?") // 00:01 mỗi đêm
    @Transactional
    public void resetBrokenStreaks() {
        userRepository.resetBrokenStreaks();
    }
}