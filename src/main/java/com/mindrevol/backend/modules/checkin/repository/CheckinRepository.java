package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// [UUID] Extends String
@Repository
public interface CheckinRepository extends JpaRepository<Checkin, String> {

    // [UUID] Tham số journeyId là String
    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") String journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(String journeyId, String userId);
    
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(String journeyId);

    List<Checkin> findByJourneyIdAndUserId(String journeyId, String id);
    
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(String userId);

    // [UUID] userId, excludedUserIds là String
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeed(@Param("userId") String userId, 
                                  @Param("cursor") LocalDateTime cursor, 
                                  @Param("excludedUserIds") Collection<String> excludedUserIds, 
                                  Pageable pageable);

    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") String journeyId, 
                                          @Param("cursor") LocalDateTime cursor, 
                                          @Param("excludedUserIds") Collection<String> excludedUserIds, 
                                          Pageable pageable);
    
    @Query("SELECT c.createdAt FROM Checkin c " +
            "WHERE c.journey.id = :journeyId " +
            "AND c.user.id = :userId " +
            "AND c.status IN ('NORMAL', 'LATE', 'COMEBACK', 'REST') " + 
            "ORDER BY c.createdAt DESC")
     List<LocalDateTime> findValidCheckinDates(@Param("journeyId") String journeyId, @Param("userId") String userId);

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Checkin> findMyCheckinsInJourney(@Param("journeyId") String journeyId, @Param("userId") String userId, Pageable pageable);
}