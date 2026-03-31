package com.mindrevol.backend.modules.box.service;

import com.mindrevol.backend.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.backend.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.backend.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.backend.modules.box.dto.response.BoxResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service xử lý logic liên quan đến Box (hộp/không gian nhóm).
 */
public interface BoxService {
    // Tạo một Box mới
    BoxResponse createBox(CreateBoxRequest request, String userId);
    
    // Xem chi tiết thông tin của một Box (người xem phải có quyền)
    BoxResponse getBoxDetails(String boxId, String userId);
    
    // Lấy danh sách các Box mà người dùng đang tham gia
    Page<BoxResponse> getMyBoxes(String userId, Pageable pageable);
    
    // Cập nhật thông tin Box (tên, mô tả...)
    BoxResponse updateBox(String boxId, UpdateBoxRequest request, String userId);
    
    // Lưu trữ (Archive) Box thay vì xóa hoàn toàn
    void archiveBox(String boxId, String userId);
    
    // Thêm trực tiếp một thành viên vào Box
    void addMember(String boxId, String targetUserId, String requesterId);
    
    // Xóa một thành viên khỏi Box (hoặc tự rời khỏi Box)
    void removeMember(String boxId, String targetUserId, String requesterId);
    
    // Giải tán (xóa) Box, chỉ dành cho Owner
    void disbandBox(String boxId, String userId);
    
    // Chuyển quyền chủ sở hữu Box cho người khác
    void transferOwnership(String boxId, String newOwnerId, String requesterId);
    
    // Lấy danh sách thành viên trong Box có phân trang
    Page<BoxMemberResponse> getBoxMembers(String boxId, String userId, Pageable pageable);
    
    // Lấy danh sách các Hành trình (Journeys) thuộc về Box này
    Page<JourneyResponse> getBoxJourneys(String boxId, String userId, Pageable pageable);
    
    // Mời một người dùng khác tham gia Box
    void inviteMember(String boxId, String targetUserId, String requesterId);
    
    // Chấp nhận lời mời tham gia Box
    void acceptInvite(String boxId, String userId);
    
    // Từ chối lời mời tham gia Box
    void rejectInvite(String boxId, String userId);
}