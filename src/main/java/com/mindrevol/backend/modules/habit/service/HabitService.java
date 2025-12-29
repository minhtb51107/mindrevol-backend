package com.mindrevol.backend.modules.habit.service;

import com.mindrevol.backend.modules.habit.dto.request.CreateHabitRequest;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;

public interface HabitService {
    HabitResponse createHabit(CreateHabitRequest request, User user);
    List<HabitResponse> getMyHabits(User user);
    
    // [FIX] Đổi UUID thành Long
    void markHabitCompleted(Long habitId, Long checkinId, User user);
    
    // [FIX] Đổi UUID thành Long
    void markHabitFailed(Long habitId, User user);

    // [FIX] Đổi UUID thành Long (vì Journey và Checkin giờ cũng là Long)
    void markHabitCompletedByJourney(Long journeyId, Long checkinId, User user);
    
    // [FIX] Đổi UUID thành Long
    void createHabitFromJourney(Long journeyId, String journeyName, User user);
}