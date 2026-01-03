package com.mindrevol.backend.modules.habit.repository;

import com.mindrevol.backend.modules.habit.entity.Habit;
import com.mindrevol.backend.modules.habit.entity.HabitLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// [UUID] String
@Repository
public interface HabitRepository extends JpaRepository<Habit, String> {
    
    // [UUID] userId là String
    List<Habit> findByUserIdAndArchivedFalse(String userId);

    // [UUID] userId, journeyId là String
    Optional<Habit> findByUserIdAndJourneyId(String userId, String journeyId);
}
