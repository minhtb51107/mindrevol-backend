package com.mindrevol.backend.modules.habit.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.habit.dto.request.CreateHabitRequest;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.habit.entity.Habit;
import com.mindrevol.backend.modules.habit.entity.HabitLog;
import com.mindrevol.backend.modules.habit.entity.HabitLogStatus;
import com.mindrevol.backend.modules.habit.mapper.HabitMapper;
import com.mindrevol.backend.modules.habit.repository.HabitLogRepository;
import com.mindrevol.backend.modules.habit.repository.HabitRepository;
import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HabitServiceImpl implements HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitMapper habitMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public HabitResponse createHabit(CreateHabitRequest request, User user) {
        Habit habit = Habit.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .frequency(request.getFrequency())
                .reminderTime(request.getReminderTime())
                .startDate(LocalDate.now())
                .build();
        
        habit = habitRepository.save(habit);
        
        HabitResponse response = habitMapper.toResponse(habit);
        response.setCompletedToday(false);
        return response;
    }

    @Override
    public List<HabitResponse> getMyHabits(User user) {
        // [UUID] user.getId() là String
        List<Habit> habits = habitRepository.findByUserIdAndArchivedFalse(user.getId());
        LocalDate today = LocalDate.now();

        return habits.stream().map(habit -> {
            boolean isCompleted = habitLogRepository
                    .findByHabitIdAndLogDate(habit.getId(), today)
                    .map(log -> log.getStatus() == HabitLogStatus.COMPLETED)
                    .orElse(false);
            
            HabitResponse response = habitMapper.toResponse(habit);
            response.setCompletedToday(isCompleted);
            return response;
            
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    // [UUID] String habitId, String checkinId
    public void markHabitCompleted(String habitId, String checkinId, User user) {
        Habit habit = getHabitAndCheckOwner(habitId, user);
        saveHabitLog(habit, checkinId, HabitLogStatus.COMPLETED);

        if (habit.getJourneyId() != null && checkinId == null) {
            log.info("Habit completed manually. Associated Journey ID: {}", habit.getJourneyId());
        }
    }

    @Override
    @Transactional
    // [UUID] String habitId
    public void markHabitFailed(String habitId, User user) {
        Habit habit = getHabitAndCheckOwner(habitId, user);
        saveHabitLog(habit, null, HabitLogStatus.FAILED);
    }

    @Override
    @Transactional
    // [UUID] String journeyId, String checkinId
    public void markHabitCompletedByJourney(String journeyId, String checkinId, User user) {
        Optional<Habit> habitOpt = habitRepository.findByUserIdAndJourneyId(user.getId(), journeyId);

        if (habitOpt.isPresent()) {
            saveHabitLog(habitOpt.get(), checkinId, HabitLogStatus.COMPLETED);
            log.info("Auto-completed Habit linked to Journey {} for User {}", journeyId, user.getId());
        } else {
            log.warn("No habit found linking to Journey {} for User {}", journeyId, user.getId());
        }
    }

    @Override
    @Transactional
    // [UUID] String journeyId
    public void createHabitFromJourney(String journeyId, String journeyName, User user) {
        if (habitRepository.findByUserIdAndJourneyId(user.getId(), journeyId).isPresent()) {
            return; 
        }

        Habit habit = Habit.builder()
                .user(user)
                .title(journeyName)
                .description("Thói quen từ hành trình: " + journeyName)
                .frequency("DAILY")
                .journeyId(journeyId)
                .startDate(LocalDate.now())
                .build();

        habitRepository.save(habit);
        log.info("Auto-created Habit from Journey {} for User {}", journeyId, user.getId());
    }

    // [UUID] String habitId
    private Habit getHabitAndCheckOwner(String habitId, User user) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found"));
        // [UUID] So sánh 2 chuỗi String
        if (!habit.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized access to habit");
        }
        return habit;
    }

    // [UUID] String checkinId
    private void saveHabitLog(Habit habit, String checkinId, HabitLogStatus status) {
        LocalDate today = LocalDate.now();
        Optional<HabitLog> existingLog = habitLogRepository.findByHabitIdAndLogDate(habit.getId(), today);

        HabitLog log = existingLog.orElse(HabitLog.builder()
                .habit(habit)
                .logDate(today)
                .build());
        
        log.setStatus(status);
        if (checkinId != null) {
            log.setCheckinId(checkinId);
        }
        
        habitLogRepository.save(log);
    }
}