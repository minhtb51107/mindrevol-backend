package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyTaskRepository extends JpaRepository<JourneyTask, UUID> {
    
    // Tìm nhiệm vụ của một ngày cụ thể trong hành trình
    Optional<JourneyTask> findByJourneyIdAndDayNo(UUID journeyId, Integer dayNo);
}