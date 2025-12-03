package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection; // Import thêm
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {

    // --- Giữ nguyên các query cũ không liên quan ---
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id = :journeyId " +
           "ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") UUID journeyId, Pageable pageable);

    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(UUID journeyId, Long userId);

    boolean existsByUserIdAndTaskId(Long userId, UUID taskId);

    @Query("SELECT c.task.id FROM Checkin c WHERE c.user.id = :userId AND c.journey.id = :journeyId AND c.task IS NOT NULL")
    Set<UUID> findCompletedTaskIdsByUserAndJourney(@Param("userId") Long userId, @Param("journeyId") UUID journeyId);

    List<Checkin> findByJourneyIdAndUserId(UUID journeyId, Long id);
    
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // --- CÁC QUERY ĐƯỢC TỐI ƯU (SỬA LẠI) ---

    /**
     * 1. Feed Tổng Hợp (Unified Feed):
     * - Logic chặn (NOT IN) đã được chuyển ra ngoài Service để tối ưu DB.
     * - Tham số: excludedUserIds (Danh sách người bị chặn/chặn mình).
     */
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + // <--- Thay đổi ở đây
           "ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeed(@Param("userId") Long userId, 
                                  @Param("cursor") LocalDateTime cursor, 
                                  @Param("excludedUserIds") Collection<Long> excludedUserIds, // <--- Tham số mới
                                  Pageable pageable);

    /**
     * 2. Feed Journey Đơn Lẻ:
     */
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.createdAt < :cursor " +
           "AND u.id NOT IN :excludedUserIds " + // <--- Thay đổi ở đây
           "ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") UUID journeyId, 
                                          @Param("cursor") LocalDateTime cursor, 
                                          @Param("excludedUserIds") Collection<Long> excludedUserIds, // <--- Tham số mới
                                          Pageable pageable);
    
    // --- Query tìm checkin nhiều like nhất (giữ nguyên hoặc tối ưu sau) ---
    @Query("SELECT c FROM Checkin c " +
            "LEFT JOIN CheckinReaction r ON c.id = r.checkin.id " +
            "WHERE c.journey.id = :journeyId AND c.user.id = :userId " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(r.id) DESC")
     List<Checkin> findMostLikedCheckins(@Param("journeyId") UUID journeyId, 
                                         @Param("userId") Long userId, 
                                         Pageable pageable);
}