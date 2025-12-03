package com.mindrevol.backend.modules.habit.listener;

import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.journey.entity.JourneyType;
import com.mindrevol.backend.modules.journey.event.JourneyCreatedEvent;
import com.mindrevol.backend.modules.journey.event.JourneyJoinedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JourneyEventListener {

    private final HabitService habitService;

    // Lắng nghe sự kiện TẠO Journey
    @Async
    @EventListener
    @Transactional
    public void handleJourneyCreated(JourneyCreatedEvent event) {
        // Chỉ tạo Habit tự động nếu loại Journey là HABIT
        if (event.getJourney().getType() == JourneyType.HABIT) {
            log.info("Creating Habit for Creator of Journey: {}", event.getJourney().getName());
            habitService.createHabitFromJourney(
                    event.getJourney().getId(), 
                    event.getJourney().getName(), 
                    event.getCreator()
            );
        }
    }

    // Lắng nghe sự kiện JOIN Journey
    @Async
    @EventListener
    @Transactional
    public void handleJourneyJoined(JourneyJoinedEvent event) {
        // Chỉ tạo Habit tự động nếu loại Journey là HABIT
        if (event.getJourney().getType() == JourneyType.HABIT) {
            log.info("Creating Habit for Participant of Journey: {}", event.getJourney().getName());
            habitService.createHabitFromJourney(
                    event.getJourney().getId(), 
                    event.getJourney().getName(), 
                    event.getParticipant()
            );
        }
    }
}