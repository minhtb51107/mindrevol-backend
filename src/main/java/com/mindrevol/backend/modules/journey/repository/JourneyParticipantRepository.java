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

    // Lấy tất cả lịch sử tham gia (cả cũ và mới)
    @Query("SELECT jp FROM JourneyParticipant jp JOIN FETCH jp.journey WHERE jp.user.id = :userId ORDER BY jp.lastCheckinAt DESC")
    List<JourneyParticipant> findAllByUserId(@Param("userId") String userId);
    
    // Lấy danh sách đang active (để hiển thị UI)
    @Query("SELECT jp FROM JourneyParticipant jp " +
            "JOIN FETCH jp.journey j " +
            "WHERE jp.user.id = :userId " +
            "AND j.status IN ('ONGOING', 'UPCOMING') " +
            "AND j.deletedAt IS NULL " +
            "ORDER BY j.startDate DESC")
    List<JourneyParticipant> findAllActiveByUserId(@Param("userId") String userId);

    List<JourneyParticipant> findAllByJourneyId(String journeyId);

    boolean existsByJourneyIdAndUserId(String journeyId, String userId);
    
    Optional<JourneyParticipant> findByJourneyIdAndUserId(String journeyId, String userId);
    
    long countByJourneyId(String journeyId);

    // [FIX QUAN TRỌNG] Sửa lại hàm này. 
    // Trước đây nó không có @Query nên nó đếm lung tung.
    // Giờ ta ép nó chỉ đếm hành trình Đang diễn ra hoặc Sắp diễn ra.
    @Query("SELECT COUNT(jp) FROM JourneyParticipant jp " +
           "JOIN jp.journey j " +
           "WHERE jp.user.id = :userId " +
           "AND j.status IN ('ONGOING', 'UPCOMING') " +
           "AND j.deletedAt IS NULL")
    long countActiveByUserId(@Param("userId") String userId);

    // Hàm job nhắc nhở (giữ nguyên)
    @Query("SELECT jp FROM JourneyParticipant jp " +
            "JOIN FETCH jp.journey j " +
            "JOIN FETCH jp.user u " +
            "WHERE j.status = 'ONGOING' " +
            "AND (jp.lastCheckinAt IS NULL OR jp.lastCheckinAt < :startOfToday)")
    Slice<JourneyParticipant> findParticipantsToRemind(@Param("startOfToday") LocalDateTime startOfToday, Pageable pageable);
}