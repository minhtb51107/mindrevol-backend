package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.request.UpdateCheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.checkin.dto.response.MapMarkerResponse; 
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý các bài đăng dạng Check-in (kèm vị trí, hình ảnh...).
 */
public interface CheckinService {

    // Tạo một check-in mới
    CheckinResponse createCheckin(CheckinRequest request, User currentUser);

    // Lấy bảng tin (feed) của một Journey cụ thể (phân trang truyền thống)
    Page<CheckinResponse> getJourneyFeed(String journeyId, Pageable pageable, User currentUser);

    // Đăng bình luận vào một bài check-in
    CommentResponse postComment(String checkinId, String content, User currentUser);
    
    // Lấy danh sách bình luận của bài check-in
    Page<CommentResponse> getComments(String checkinId, Pageable pageable);

    // Lấy bảng tin tổng hợp (Unified Feed) kết hợp từ các nguồn (bạn bè, box, journey), sử dụng Cursor Pagination
    List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit);

    // Lấy bảng tin của Journey sử dụng Cursor Pagination (hiệu suất cao hơn phân trang truyền thống)
    List<CheckinResponse> getJourneyFeedByCursor(String journeyId, User currentUser, LocalDateTime cursor, int limit);
    
    // Cập nhật nội dung (caption) của bài check-in
    CheckinResponse updateCheckin(String checkinId, UpdateCheckinRequest request, User currentUser);

    // Xóa bài check-in
    void deleteCheckin(String checkinId, User currentUser);

    // [THÊM MỚI] Lấy danh sách các điểm đánh dấu trên bản đồ cho một Journey
    List<MapMarkerResponse> getMapMarkersForJourney(String journeyId, User currentUser);
    
    // [THÊM MỚI] Lấy danh sách các điểm đánh dấu trên bản đồ cho một Box
    List<MapMarkerResponse> getMapMarkersForBox(String boxId, User currentUser);
    
    // Lấy danh sách các điểm đánh dấu trên bản đồ của chính người dùng
    List<MapMarkerResponse> getMyMapMarkers(User currentUser);

	Page<CheckinResponse> getArchivedCheckins(User currentUser, Pageable pageable);
}