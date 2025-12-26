package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {

    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id = :journeyId " +
           "ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") UUID journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(UUID journeyId, Long userId);
    
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(UUID journeyId);

    boolean existsByUserIdAndTaskId(Long userId, UUID taskId);

    @Query("SELECT c.task.id FROM Checkin c WHERE c.user.id = :userId AND c.journey.id = :journeyId AND c.task IS NOT NULL")
    Set<UUID> findCompletedTaskIdsByUserAndJourney(@Param("userId") Long userId, @Param("journeyId") UUID journeyId);

    List<Checkin> findByJourneyIdAndUserId(UUID journeyId, Long id);
    
    // Hỗ trợ export data
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 1. Feed Tổng Hợp (Unified Feed):
     * - Load trước User và Task để tránh N+1 Query.
     * - Subquery lấy danh sách Journey mà user tham gia.
     * - Lọc các user bị chặn (excludedUserIds).
     */
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeed(@Param("userId") Long userId, 
                                  @Param("cursor") LocalDateTime cursor, 
                                  @Param("excludedUserIds") Collection<Long> excludedUserIds, 
                                  Pageable pageable);

    /**
     * 2. Feed Journey Đơn Lẻ:
     * - Tương tự như trên nhưng chỉ cho 1 Journey cụ thể.
     */
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + 
           "ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") UUID journeyId, 
                                          @Param("cursor") LocalDateTime cursor, 
                                          @Param("excludedUserIds") Collection<Long> excludedUserIds, 
                                          Pageable pageable);
    
    /**
     * Tìm checkin nhiều like nhất trong 1 journey của user (Dùng cho Recap)
     */
    @Query("SELECT c FROM Checkin c " +
            "LEFT JOIN CheckinReaction r ON c.id = r.checkin.id " +
            "WHERE c.journey.id = :journeyId AND c.user.id = :userId " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(r.id) DESC")
     List<Checkin> findMostLikedCheckins(@Param("journeyId") UUID journeyId, 
                                         @Param("userId") Long userId, 
                                         Pageable pageable);
    
 // Thêm query loại trừ danh sách chặn
    @Query("SELECT c FROM Checkin c WHERE c.visibility = 'PUBLIC' AND c.user.id NOT IN :blockedUserIds ORDER BY c.createdAt DESC")
    Page<Checkin> findAllFeedExcudingBlocked(List<Long> blockedUserIds, Pageable pageable);
    
    @Query("SELECT c.createdAt FROM Checkin c " +
            "WHERE c.journey.id = :journeyId " +
            "AND c.user.id = :userId " +
            "AND c.status IN ('NORMAL', 'LATE', 'COMEBACK', 'REST') " + // Chỉ lấy các trạng thái tính là "có mặt"
            "ORDER BY c.createdAt DESC")
     List<LocalDateTime> findValidCheckinDates(@Param("journeyId") UUID journeyId, @Param("userId") Long userId);
    
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Checkin> findMyCheckinsInJourney(@Param("journeyId") UUID journeyId, 
                                          @Param("userId") Long userId, 
                                          Pageable pageable);
    
    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId AND c.checkinDate = :date")
    List<Checkin> findAllByJourneyIdAndCheckinDate(@Param("journeyId") UUID journeyId, @Param("date") LocalDate date);
    
    List<Checkin> findByJourneyIdAndUserIdOrderByCheckinDateDesc(UUID journeyId, Long userId);
}