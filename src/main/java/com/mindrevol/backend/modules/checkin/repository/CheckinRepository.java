package com.mindrevol.backend.modules.checkin.repository;

import com.mindrevol.backend.modules.checkin.dto.response.CalendarRecapResponse;
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

    // [THÊM MỚI] Lấy các bài viết Lưu trữ cá nhân (không thuộc hành trình nào)
    @Query("SELECT c FROM Checkin c WHERE c.user.id = :userId AND c.journey IS NULL ORDER BY c.createdAt DESC")
    Page<Checkin> findArchivedCheckinsByUser(@Param("userId") String userId, Pageable pageable);

    // Phân trang lấy tất cả bài check-in trong một Hành trình (của tất cả mọi người)
    @Query("SELECT c FROM Checkin c JOIN FETCH c.user WHERE c.journey.id = :journeyId ORDER BY c.createdAt DESC")
    Page<Checkin> findByJourneyIdOrderByCreatedAtDesc(@Param("journeyId") String journeyId, Pageable pageable);

    // Lấy bài check-in mới nhất của một user trong một hành trình cụ thể
    Optional<Checkin> findTopByJourneyIdAndUserIdOrderByCreatedAtDesc(String journeyId, String userId);
    
    // Lấy tất cả bài check-in trong một hành trình, sắp xếp theo ngày check-in thực tế
    List<Checkin> findByJourneyIdOrderByCheckinDateDesc(String journeyId);

    // Lấy tất cả bài check-in của một user cụ thể trong một hành trình
    List<Checkin> findByJourneyIdAndUserId(String journeyId, String id);
    
    // Lấy lịch sử check-in của một user trên toàn hệ thống
    List<Checkin> findAllByUserIdOrderByCreatedAtDesc(String userId);

    // [THÊM MỚI - LOGIC TÀNG HÌNH]: Nếu người xem là người lạ (isViewerMember = false), 
    // chỉ lấy những bài check-in của những user đã bật "Hiển thị công khai" (isProfileVisible = true)
    @Query("SELECT c FROM Checkin c " +
           "JOIN JourneyParticipant jp ON c.journey.id = jp.journey.id AND c.user.id = jp.user.id " +
           "WHERE c.journey.id = :journeyId " +
           "AND (:isViewerMember = true OR jp.isProfileVisible = true) " +
           "ORDER BY c.createdAt DESC")
    List<Checkin> findVisibleCheckinsByJourneyId(@Param("journeyId") String journeyId, @Param("isViewerMember") boolean isViewerMember);

    // Lấy bảng tin tổng hợp (Unified Feed) sử dụng Cursor Pagination (hiệu suất cao hơn Page/Offset).
    // Bỏ qua các user nằm trong danh sách excludedUserIds (thường là những người bị block).
    @Query("SELECT c FROM Checkin c JOIN FETCH c.user u WHERE c.journey.id IN (SELECT p.journey.id FROM JourneyParticipant p WHERE p.user.id = :userId) AND c.createdAt >= :sinceDate AND c.createdAt <= :cursor AND u.id NOT IN :excludedUserIds ORDER BY c.createdAt DESC")
    List<Checkin> findUnifiedFeedRecent(@Param("userId") String userId, @Param("sinceDate") LocalDateTime sinceDate, @Param("cursor") LocalDateTime cursor, @Param("excludedUserIds") Collection<String> excludedUserIds, Pageable pageable);

    // Cursor Pagination lấy bảng tin dành riêng cho một hành trình
    @Query("SELECT c FROM Checkin c JOIN FETCH c.user u WHERE c.journey.id = :journeyId AND c.createdAt <= :cursor AND u.id NOT IN :excludedUserIds ORDER BY c.createdAt DESC")
    List<Checkin> findJourneyFeedByCursor(@Param("journeyId") String journeyId, @Param("cursor") LocalDateTime cursor, @Param("excludedUserIds") Collection<String> excludedUserIds, Pageable pageable);
    
    // Tìm các ngày mà user ĐÃ THỰC SỰ check-in hợp lệ (không tính trạng thái vắng mặt)
    @Query("SELECT c.createdAt FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId AND c.status IN ('NORMAL', 'LATE', 'COMEBACK', 'REST') ORDER BY c.createdAt DESC")
    List<LocalDateTime> findValidCheckinDates(@Param("journeyId") String journeyId, @Param("userId") String userId);

    // Phân trang lấy danh sách bài check-in của chính mình trong 1 hành trình
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Checkin> findMyCheckinsInJourney(@Param("journeyId") String journeyId, @Param("userId") String userId, Pageable pageable);

    // Lấy các bài check-in CÓ HÌNH ẢNH để hiển thị trong mục Thư viện/Gallery của hành trình
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.status <> 'REJECTED'")
    List<Checkin> findMediaByJourneyId(@Param("journeyId") String journeyId);

    // --- CÁC HÀM LẤY TỌA ĐỘ BẢN ĐỒ (MARKERS) ---
    // Bản đồ hành trình (chỉ lấy bài có tọa độ GPS)
    @Query("SELECT c FROM Checkin c WHERE c.journey.id = :journeyId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByJourney(@Param("journeyId") String journeyId);

    // Bản đồ Box (tổng hợp tọa độ của tất cả hành trình trong Box)
    @Query("SELECT c FROM Checkin c WHERE c.journey.box.id = :boxId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByBox(@Param("boxId") String boxId);
    
    // Bản đồ cá nhân của User
    @Query("SELECT c FROM Checkin c WHERE c.user.id = :userId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status <> 'REST'")
    List<Checkin> findMapMarkersByUser(@Param("userId") String userId);
    
    // Lấy nhanh một vài hình ảnh preview của hành trình
    @org.springframework.data.jpa.repository.Query("SELECT c.imageUrl FROM Checkin c WHERE c.journey.id = :journeyId AND c.imageUrl IS NOT NULL AND c.imageUrl != '' ORDER BY c.createdAt DESC")
    java.util.List<String> findPreviewImagesByJourneyId(@org.springframework.data.repository.query.Param("journeyId") String journeyId, org.springframework.data.domain.Pageable pageable);
    
    // Truy vấn Native SQL phức tạp: Lấy DỮ LIỆU LỊCH (Calendar Recap) cho một tháng cụ thể.
    // Dùng ROW_NUMBER() để đảm bảo: Nếu 1 ngày đăng nhiều bài, chỉ lấy 1 bài (bài mới nhất) để làm ảnh đại diện cho ngày đó trên Lịch.
    @Query(value = "SELECT " +
            "  CAST(EXTRACT(DAY FROM created_at) AS INTEGER) AS day, " +
            "  image_url AS imageUrl, " +
            "  id AS checkinId " +
            "FROM (" +
            "  SELECT id, created_at, image_url, " +
            "         ROW_NUMBER() OVER(PARTITION BY EXTRACT(DAY FROM created_at) ORDER BY created_at DESC) as rn " +
            "  FROM checkins " +
            "  WHERE user_id = :userId " +
            "    AND EXTRACT(MONTH FROM created_at) = :month " +
            "    AND EXTRACT(YEAR FROM created_at) = :year " +
            "    AND deleted_at IS NULL " +
            "    AND image_url IS NOT NULL " +
            ") AS subquery " +
            "WHERE rn = 1", 
    nativeQuery = true)
    List<CalendarRecapResponse> getCalendarRecapInMonth(@Param("userId") String userId, 
                                                 @Param("year") int year, 
                                                 @Param("month") int month);
    
    // Đếm tổng số bài đăng của một user (bỏ qua các bài đã xóa mềm - Soft Delete)
    long countByUserIdAndDeletedAtIsNull(String userId);
    
    // Đếm tổng số bài đăng của user (Dùng khi DB không áp dụng xóa mềm)
    long countByUserId(String userId);
}