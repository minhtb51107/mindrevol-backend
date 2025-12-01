package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JourneyParticipantRepository extends JpaRepository<JourneyParticipant, UUID> {
    
    @Query("SELECT jp FROM JourneyParticipant jp JOIN FETCH jp.journey WHERE jp.user.id = :userId")
    List<JourneyParticipant> findAllByUserId(Long userId);

    boolean existsByJourneyIdAndUserId(UUID journeyId, Long userId);
    
    Optional<JourneyParticipant> findByJourneyIdAndUserId(UUID journeyId, Long userId);
    
    @Modifying
    @Query(value = """
        UPDATE journey_participants 
        SET current_streak = 0 
        WHERE current_streak > 0 
        AND id NOT IN (
            SELECT p.id 
            FROM journey_participants p
            JOIN checkins c ON c.journey_id = p.journey_id AND c.user_id = p.user_id
            WHERE c.created_at >= CURRENT_DATE - INTERVAL '1 DAY'
        )
    """, nativeQuery = true)
    void resetStreakForInactiveUsers();

	List<JourneyParticipant> findAllByJourneyId(UUID id);
}