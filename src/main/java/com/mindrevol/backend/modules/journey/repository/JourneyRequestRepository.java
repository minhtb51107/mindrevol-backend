package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyRequest;
import com.mindrevol.backend.modules.journey.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// [FIX] JpaRepository<..., Long> thay vì UUID
@Repository
public interface JourneyRequestRepository extends JpaRepository<JourneyRequest, Long> {
    
    // [FIX] Tất cả UUID -> Long
    Optional<JourneyRequest> findByJourneyIdAndUserId(Long journeyId, Long userId);
    
    List<JourneyRequest> findByJourneyIdAndStatus(Long journeyId, RequestStatus status);
    
    boolean existsByJourneyIdAndUserIdAndStatus(Long journeyId, Long userId, RequestStatus status);
};