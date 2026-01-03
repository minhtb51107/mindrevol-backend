package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyRequest;
import com.mindrevol.backend.modules.journey.entity.RequestStatus;
import com.mindrevol.backend.modules.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyRequestRepository extends JpaRepository<JourneyRequest, String> { // [UUID]
    
    // [UUID] String
    Optional<JourneyRequest> findByJourneyIdAndUserId(String journeyId, String userId);
    
    List<JourneyRequest> findByJourneyIdAndStatus(String journeyId, RequestStatus status);
    
    boolean existsByJourneyIdAndUserIdAndStatus(String journeyId, String userId, RequestStatus status);

	List<JourneyRequest> findAllByJourneyIdAndStatus(String journeyId, RequestStatus pending);

	List<JourneyRequest> findAllByUserIdAndStatus(String userId, RequestStatus status);

	long countByJourneyIdAndStatus(String jId, RequestStatus pending);
}