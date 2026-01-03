package com.mindrevol.backend.modules.habit.repository;

import com.mindrevol.backend.modules.habit.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

// [UUID] String
@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, String> {
    // [UUID] habitId là String
    Optional<HabitLog> findByHabitIdAndLogDate(String habitId, LocalDate logDate);
}