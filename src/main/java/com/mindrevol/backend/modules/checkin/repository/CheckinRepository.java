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
public interface CheckinRepository extends JpaRepository<Checkin, String> {

    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") String journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(String journeyId, String userId);
    
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(String journeyId);

    List<Checkin> findByJourneyIdAndUserId(String journeyId, String id);
    
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) " +
           "AND c.createdAt >= :sinceDate " + 
           "AND c.createdAt <= :cursor " +    
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeedRecent(@Param("userId") String userId, 
                                        @Param("sinceDate") LocalDateTime sinceDate,
                                        @Param("cursor") LocalDateTime cursor, 
                                        @Param("excludedUserIds") Collection<String> excludedUserIds, 
                                        Pageable pageable);

    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.createdAt <= :cursor " + 
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

    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.status <> 'REJECTED'")
    List<Checkin> findMediaByJourneyId(@Param("journeyId") String journeyId);

    // =========================================================================
    //  [THÊM MỚI] BẢN ĐỒ KỶ NIỆM (MAP MARKERS)
    // =========================================================================

    // Lấy các điểm đánh dấu trên bản đồ cho 1 Hành trình cụ thể
    @Query("SELECT c FROM Checkin c " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByJourney(@Param("journeyId") String journeyId);

    // Lấy các điểm đánh dấu trên bản đồ cho TOÀN BỘ Không gian (Box)
    @Query("SELECT c FROM Checkin c " +
           "WHERE c.journey.box.id = :boxId " +
           "AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByBox(@Param("boxId") String boxId);
    
 // Lấy tất cả các điểm đánh dấu trên bản đồ của một User
    @Query("SELECT c FROM Checkin c " +
           "WHERE c.user.id = :userId " +
           "AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByUser(@Param("userId") String userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT c.imageUrl FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.imageUrl != '' ORDER BY c.createdAt DESC")
    java.util.List<String> findPreviewImagesByJourneyId(@org.springframework.data.repository.query.Param("journeyId") String journeyId, org.springframework.data.domain.Pageable pageable);
}