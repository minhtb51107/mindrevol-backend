package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    boolean existsByUserIdAndTaskId(Long userId, UUID taskId);

    @Query("SELECT c.task.id FROM Checkin c WHERE c.user.id = :userId AND c.journey.id = :journeyId AND c.task IS NOT NULL")
    Set<UUID> findCompletedTaskIdsByUserAndJourney(@Param("userId") Long userId, @Param("journeyId") UUID journeyId);

    List<Checkin> findByJourneyIdAndUserId(UUID journeyId, Long id);

    // --- CÁC QUERY MỚI (TỐI ƯU FEED) ---

    /**
     * 1. Feed Tổng Hợp (Unified Feed):
     * - Lấy bài từ Journey đã tham gia.
     * - Loại bỏ bài của người TÔI CHẶN (blocker = me).
     * - Loại bỏ bài của người CHẶN TÔI (blocked = me).
     */
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) " +
           "AND c.createdAt < :cursor " +
           // --- LOGIC BLOCK ---
           "AND u.id NOT IN (SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :userId) " + // Ko lấy bài người mình chặn
           "AND u.id NOT IN (SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :userId) " + // Ko lấy bài người chặn mình
           // -------------------
           "ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeed(@Param("userId") Long userId, 
                                  @Param("cursor") LocalDateTime cursor, 
                                  Pageable pageable);

    /**
     * 2. Feed Journey Đơn Lẻ:
     * - Cũng áp dụng logic tương tự để đảm bảo sạch sẽ.
     */
    @Query("SELECT c FROM Checkin c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.task " +
           "WHERE c.journey.id = :journeyId " +
           "AND c.createdAt < :cursor " +
           // --- LOGIC BLOCK ---
           "AND u.id NOT IN (SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :userId) " +
           "AND u.id NOT IN (SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :userId) " +
           // -------------------
           "ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") UUID journeyId, 
                                          @Param("userId") Long userId, // <--- Nhớ thêm param này vào Service gọi
                                          @Param("cursor") LocalDateTime cursor, 
                                          Pageable pageable);
    
    @Query("SELECT c FROM Checkin c " +
            "LEFT JOIN CheckinReaction r ON c.id = r.checkin.id " +
            "WHERE c.journey.id = :journeyId AND c.user.id = :userId " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(r.id) DESC")
     List<Checkin> findMostLikedCheckins(@Param("journeyId") UUID journeyId, 
                                         @Param("userId") Long userId, 
                                         Pageable pageable);
    
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}