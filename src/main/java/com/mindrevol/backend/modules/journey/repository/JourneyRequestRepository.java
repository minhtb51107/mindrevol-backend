package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyRequest;
import com.mindrevol.backend.modules.journey.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyRequestRepository extends JpaRepository<JourneyRequest, UUID> {
    Optional<JourneyRequest> findByJourneyIdAndUserId(UUID journeyId, Long userId);
    List<JourneyRequest> findByJourneyIdAndStatus(UUID journeyId, RequestStatus status);
}