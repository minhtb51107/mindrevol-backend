package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import org.springframework.data.domain.Page;
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
public interface JourneyParticipantRepository extends JpaRepository<JourneyParticipant, Long> {

    @Query("SELECT jp FROM JourneyParticipant jp JOIN FETCH jp.journey WHERE jp.user.id = :userId ORDER BY jp.lastCheckinAt DESC")
    List<JourneyParticipant> findAllByUserId(@Param("userId") Long userId);
    
    Page<JourneyParticipant> findAllByUserId(Long userId, Pageable pageable);

    List<JourneyParticipant> findAllByJourneyId(Long journeyId);

    boolean existsByJourneyIdAndUserId(Long journeyId, Long userId);
    
    Optional<JourneyParticipant> findByJourneyIdAndUserId(Long journeyId, Long userId);
    
    long countByJourneyId(Long journeyId);

    @Query("SELECT COUNT(jp) FROM JourneyParticipant jp JOIN jp.journey j " +
           "WHERE jp.user.id = :userId AND j.status = 'ONGOING'")
    long countActiveJourneysByUserId(@Param("userId") Long userId);

    // [THÊM MỚI] Query tìm người cần nhắc nhở
    // Logic: Hành trình đang diễn ra (ONGOING) VÀ (Chưa checkin bao giờ HOẶC checkin lần cuối trước 00:00 hôm nay)
    // Dùng JOIN FETCH user để khi job gửi noti không bị lỗi Lazy Loading user ID
    @Query("SELECT jp FROM JourneyParticipant jp " +
           "JOIN FETCH jp.journey j " +
           "JOIN FETCH jp.user u " +
           "WHERE j.status = 'ONGOING' " +
           "AND (jp.lastCheckinAt IS NULL OR jp.lastCheckinAt < :startOfToday)")
    Slice<JourneyParticipant> findParticipantsToRemind(@Param("startOfToday") LocalDateTime startOfToday, Pageable pageable);
}