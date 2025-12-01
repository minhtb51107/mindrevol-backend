package com.mindrevol.backend.modules.habit.listener;

import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HabitEventListener {

    private final HabitService habitService;
    private final UserRepository userRepository;

    @Async
    @EventListener
    @Transactional
    public void handleCheckinSuccess(CheckinSuccessEvent event) {
        log.info("Habit Module nhận sự kiện Checkin: user={}, journey={}", event.getUserId(), event.getJourneyId());

        // Cần lấy User entity vì HabitService đang cần tham số User
        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Gọi hàm đánh dấu hoàn thành Habit theo Journey
        habitService.markHabitCompletedByJourney(
                event.getJourneyId(), 
                event.getCheckinId(), 
                user
        );
    }
}