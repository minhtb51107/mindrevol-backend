package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
    // --- SỬA LOGIC: Thêm điều kiện lọc deleted_at cho native query ---
    @Query(value = """
        UPDATE journey_participants 
        SET current_streak = 0 
        WHERE current_streak > 0 
        AND deleted_at IS NULL  -- <--- QUAN TRỌNG
        AND id NOT IN (
            SELECT p.id 
            FROM journey_participants p
            JOIN checkins c ON c.journey_id = p.journey_id AND c.user_id = p.user_id
            WHERE c.created_at >= CURRENT_DATE - INTERVAL '1 DAY'
            AND c.deleted_at IS NULL -- <--- QUAN TRỌNG: Checkin cũng phải chưa xóa
        )
    """, nativeQuery = true)
    void resetStreakForInactiveUsers();
    // ----------------------------------------------------------------

	List<JourneyParticipant> findAllByJourneyId(UUID id);
	
    // Các JPQL Query bên dưới không cần sửa vì Entity đã có @Where(clause = "deleted_at IS NULL")
    // Hibernate sẽ tự động thêm điều kiện vào JPQL.
    @Query("SELECT jp FROM JourneyParticipant jp " +
           "JOIN FETCH jp.journey j " +
           "JOIN FETCH jp.user u " +
           "WHERE j.hasStreak = true " +
           "AND jp.currentStreak > 0 " +
           "AND (jp.lastCheckinAt IS NULL OR jp.lastCheckinAt < :yesterday)")
    Slice<JourneyParticipant> findParticipantsToResetStreak(@Param("yesterday") LocalDate yesterday, Pageable pageable);

    @Query("SELECT jp FROM JourneyParticipant jp " +
           "JOIN FETCH jp.journey j " +
           "JOIN FETCH jp.user u " +
           "WHERE j.status = 'ACTIVE' " +
           "AND (jp.lastCheckinAt IS NULL OR jp.lastCheckinAt < :today)")
    Slice<JourneyParticipant> findParticipantsToRemind(@Param("today") LocalDate today, Pageable pageable);
}