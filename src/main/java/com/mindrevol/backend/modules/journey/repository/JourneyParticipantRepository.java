package com.mindrevol.backend.modules.journey.repository;

import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourneyParticipantRepository extends JpaRepository<JourneyParticipant, String> {

    // Lấy tất cả lịch sử tham gia hành trình (cả cũ đã đóng và mới) của một người
    @Query("SELECT jp FROM JourneyParticipant jp JOIN FETCH jp.journey WHERE jp.user.id = :userId ORDER BY jp.lastCheckinAt DESC")
    List<JourneyParticipant> findAllByUserId(@Param("userId") String userId);
    
    // Lấy danh sách record tham gia của những Hành trình đang chạy/sắp chạy (để render UI)
    @Query("SELECT jp FROM JourneyParticipant jp " +
            "JOIN FETCH jp.journey j " +
            "WHERE jp.user.id = :userId " +
            "AND j.status IN ('ONGOING', 'UPCOMING') " +
            "AND j.deletedAt IS NULL " +
            "ORDER BY j.startDate DESC")
    List<JourneyParticipant> findAllActiveByUserId(@Param("userId") String userId);

    // Lấy tất cả thành viên trong một Hành trình cụ thể
    List<JourneyParticipant> findAllByJourneyId(String journeyId);

    // Kiểm tra xem một người đã tham gia hành trình này chưa
    boolean existsByJourneyIdAndUserId(String journeyId, String userId);
    
    // Tìm record tham gia cụ thể của một người trong một hành trình
    Optional<JourneyParticipant> findByJourneyIdAndUserId(String journeyId, String userId);
    
    // Đếm tổng số người trong một hành trình
    long countByJourneyId(String journeyId);

    // [FIX QUAN TRỌNG] Chỉ đếm số lượng hành trình ACTIVE THỰC SỰ để validate tài khoản miễn phí (Limit count)
    // - Trạng thái là ONGOING hoặc UPCOMING
    // - Hành trình chưa bị xóa mềm
    // - Ngày kết thúc chưa qua
    @Query("SELECT COUNT(jp) FROM JourneyParticipant jp " +
           "JOIN jp.journey j " +
           "WHERE jp.user.id = :userId " +
           "AND j.status IN ('ONGOING', 'UPCOMING') " +
           "AND j.deletedAt IS NULL " +
           "AND (j.endDate IS NULL OR j.endDate >= :today)")
    long countActiveByUserId(@Param("userId") String userId, @Param("today") LocalDate today);

    // Hàm phục vụ cho Cron Job (Scheduled Worker):
    // Tìm những người tham gia hành trình ĐANG CHẠY nhưng HÔM NAY CHƯA CHECK-IN để gửi Push Notification nhắc nhở.
    @Query("SELECT jp FROM JourneyParticipant jp " +
            "JOIN FETCH jp.journey j " +
            "JOIN FETCH jp.user u " +
            "WHERE j.status = 'ONGOING' " +
            "AND (jp.lastCheckinAt IS NULL OR jp.lastCheckinAt < :startOfToday)")
    Slice<JourneyParticipant> findParticipantsToRemind(@Param("startOfToday") LocalDateTime startOfToday, Pageable pageable);
}