package com.mindrevol.backend.modules.habit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindrevol.backend.modules.habit.entity.Habit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {
    
    List<Habit> findByUserIdAndArchivedFalse(Long userId);

    Optional<Habit> findByUserIdAndJourneyId(Long userId, Long journeyId);
}