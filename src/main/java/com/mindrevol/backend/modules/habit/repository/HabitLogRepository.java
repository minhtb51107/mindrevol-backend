package com.mindrevol.backend.modules.habit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.habit.entity.HabitLog;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, UUID> {
    Optional<HabitLog> findByHabitIdAndLogDate(UUID habitId, LocalDate logDate);
}