package com.mindrevol.backend.modules.checkin.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalStreakListener {

    private final UserRepository userRepository;

    @EventListener
    @Transactional
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        User user = userRepository.findById(event.getUserId()).orElse(null);
        if (user == null) return;

        LocalDate today = LocalDate.now(ZoneId.of("UTC")); // Bạn có thể lấy timezone của user nếu muốn
        LocalDate lastCheckin = user.getLastCheckinAt() != null ? user.getLastCheckinAt().toLocalDate() : null;

        if (lastCheckin == null || lastCheckin.isBefore(today.minusDays(1))) {
            user.setCurrentStreak(1);
        } else if (lastCheckin.isEqual(today.minusDays(1))) {
            user.setCurrentStreak((user.getCurrentStreak() == null ? 0 : user.getCurrentStreak()) + 1);
        }

        user.setLastCheckinAt(LocalDateTime.now(ZoneId.of("UTC")));
        userRepository.save(user);
    }
}