package com.mindrevol.backend.modules.habit.service;

import com.mindrevol.backend.modules.habit.dto.request.CreateHabitRequest;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface HabitService {
    HabitResponse createHabit(CreateHabitRequest request, User user);
    List<HabitResponse> getMyHabits(User user);
    
    void markHabitCompleted(UUID habitId, UUID checkinId, User user);
    
    void markHabitFailed(UUID habitId, User user);

    void markHabitCompletedByJourney(UUID journeyId, UUID checkinId, User user);
    
    void createHabitFromJourney(UUID journeyId, String journeyName, User user);
}