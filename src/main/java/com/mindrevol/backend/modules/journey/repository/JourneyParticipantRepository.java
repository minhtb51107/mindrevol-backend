package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyParticipantRepository extends JpaRepository<JourneyParticipant, String> {

    @Query("SELECT jp FROM JourneyParticipant jp JOIN FETCH jp.journey WHERE jp.user.id = :userId ORDER BY jp.lastCheckinAt DESC")
    List<JourneyParticipant> findAllByUserId(@Param("userId") String userId);
    
    // [MỚI] Tìm các hành trình đang hoạt động (ONGOING) của user
    @Query("SELECT jp FROM JourneyParticipant jp " +
           "JOIN FETCH jp.journey j " +
           "WHERE jp.user.id = :userId AND j.status = 'ONGOING' " +
           "ORDER BY j.startDate DESC")
    List<JourneyParticipant> findAllActiveByUserId(@Param("userId") String userId);

    List<JourneyParticipant> findAllByJourneyId(String journeyId);

    boolean existsByJourneyIdAndUserId(String journeyId, String userId);
    
    Optional<JourneyParticipant> findByJourneyIdAndUserId(String journeyId, String userId);
    
    long countByJourneyId(String journeyId);

    @Query("SELECT COUNT(jp) FROM JourneyParticipant jp JOIN jp.journey j " +
           "WHERE jp.user.id = :userId AND j.status = 'ONGOING'")
    long countActiveJourneysByUserId(@Param("userId") String userId);

    @Query("SELECT jp FROM JourneyParticipant jp " +
           "JOIN FETCH jp.journey j " +
           "JOIN FETCH jp.user u " +
           "WHERE j.status = 'ONGOING' " +
           "AND (jp.lastCheckinAt IS NULL OR jp.lastCheckinAt < :startOfToday)")
    Slice<JourneyParticipant> findParticipantsToRemind(@Param("startOfToday") LocalDateTime startOfToday, Pageable pageable);

	long countActiveByUserId(String userId);
}