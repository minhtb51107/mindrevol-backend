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

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") Long journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(Long journeyId, Long userId);
    
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(Long journeyId);

    List<Checkin> findByJourneyIdAndUserId(Long journeyId, Long id);
    
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeed(@Param("userId") Long userId, 
                                  @Param("cursor") LocalDateTime cursor, 
                                  @Param("excludedUserIds") Collection<Long> excludedUserIds, 
                                  Pageable pageable);

    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") Long journeyId, 
                                          @Param("cursor") LocalDateTime cursor, 
                                          @Param("excludedUserIds") Collection<Long> excludedUserIds, 
                                          Pageable pageable);
    
    @Query("SELECT c.createdAt FROM Checkin c " +
            "WHERE c.journey.id = :journeyId " +
            "AND c.user.id = :userId " +
            "AND c.status IN ('NORMAL', 'LATE', 'COMEBACK', 'REST') " + 
            "ORDER BY c.createdAt DESC")
     List<LocalDateTime> findValidCheckinDates(@Param("journeyId") Long journeyId, @Param("userId") Long userId);

    // [FIX] Thêm @Query vào đây để Spring hiểu logic (tìm theo journeyId và userId)
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Checkin> findMyCheckinsInJourney(@Param("journeyId") Long journeyId, @Param("userId") Long userId, Pageable pageable);
}