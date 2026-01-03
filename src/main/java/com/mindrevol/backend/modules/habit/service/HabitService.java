package com.mindrevol.backend.modules.habit.service;

import com.mindrevol.backend.modules.habit.dto.request.CreateHabitRequest;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;

public interface HabitService {
    HabitResponse createHabit(CreateHabitRequest request, User user);
    List<HabitResponse> getMyHabits(User user);
    
    // [UUID] Đổi Long -> String
    void markHabitCompleted(String habitId, String checkinId, User user);
    
    // [UUID] Đổi Long -> String
    void markHabitFailed(String habitId, User user);

    // [UUID] Đổi Long -> String
    void markHabitCompletedByJourney(String journeyId, String checkinId, User user);
    
    // [UUID] Đổi Long -> String
    void createHabitFromJourney(String journeyId, String journeyName, User user);
}