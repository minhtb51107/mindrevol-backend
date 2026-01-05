package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinStatus;
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

    // 1. Lấy check-in theo hành trình (cho trang chi tiết hành trình)
    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") String journeyId, Pageable pageable);

    // 2. Tìm check-in mới nhất của user trong hành trình (để kiểm tra xem hôm nay đã check-in chưa)
    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(String journeyId, String userId);
    
    // 3. Lấy list để hiển thị dạng lịch/timeline
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(String journeyId);

    // 4. Lấy tất cả ảnh của user trong hành trình (để tính streak hoặc recap cá nhân)
    List<Checkin> findByJourneyIdAndUserId(String journeyId, String id);
    
    // 5. Lấy tất cả checkin của user (cho trang Profile cá nhân)
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(String userId);

    // =========================================================================
    //  PHẦN CORE: NEWSFEED & SOCIAL LOGIC (GIỮ NGUYÊN TỪ CODE CŨ CỦA BẠN)
    // =========================================================================

    // 6. Newsfeed tổng hợp: Lấy bài từ các hành trình mình tham gia + loại bỏ người mình block
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

    // 7. Feed của một hành trình cụ thể (có phân trang theo cursor)
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
    
    // 8. Lấy ngày check-in hợp lệ (cho Calendar View)
    // Lưu ý: Đảm bảo CheckinStatus của bạn vẫn giữ các giá trị này, hoặc đổi thành 'ACTIVE' nếu đã tối giản.
    @Query("SELECT c.createdAt FROM Checkin c " +
            "WHERE c.journey.id = :journeyId " +
            "AND c.user.id = :userId " +
            "AND c.status IN ('NORMAL', 'LATE', 'COMEBACK', 'REST') " + 
            "ORDER BY c.createdAt DESC")
     List<LocalDateTime> findValidCheckinDates(@Param("journeyId") String journeyId, @Param("userId") String userId);

    // 9. Lấy bài của tôi trong hành trình (có phân trang)
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Checkin> findMyCheckinsInJourney(@Param("journeyId") String journeyId, @Param("userId") String userId, Pageable pageable);

    // =========================================================================
    //  BỔ SUNG CHO TÍNH NĂNG RECAP (VIDEO KỶ NIỆM)
    // =========================================================================
    
    // 10. Lấy toàn bộ ảnh Active có ImageUrl trong hành trình để tạo video
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.status <> 'REJECTED'")
    List<Checkin> findMediaByJourneyId(@Param("journeyId") String journeyId);
}