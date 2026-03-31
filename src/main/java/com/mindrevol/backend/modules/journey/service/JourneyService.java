package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;

import java.util.List;

/**
 * Service xử lý các logic chính liên quan đến Hành trình (Journey/Sự kiện).
 */
public interface JourneyService {
    // Tạo mới một hành trình
    JourneyResponse createJourney(CreateJourneyRequest request, String userId);
    // Tham gia hành trình bằng mã lời mời (invite code)
    JourneyResponse joinJourney(String inviteCode, String userId);
    // Xem chi tiết hành trình
    JourneyResponse getJourneyDetail(String userId, String journeyId);
    // Lấy danh sách hành trình của tôi
    List<JourneyResponse> getMyJourneys(String userId);
    // Rời khỏi hành trình
    void leaveJourney(String journeyId, String userId);
    // Cập nhật thông tin hành trình
    JourneyResponse updateJourney(String journeyId, CreateJourneyRequest request, String userId);
    // Mời/Đuổi một thành viên khỏi hành trình
    void kickMember(String journeyId, String memberId, String requesterId);
    // Chuyển nhượng quyền chủ phòng cho người khác
    void transferOwnership(String journeyId, String currentOwnerId, String newOwnerId);
    // Lấy danh sách những người tham gia hành trình
    List<JourneyParticipantResponse> getJourneyParticipants(String journeyId);
    // Xóa/Kết thúc hành trình
    void deleteJourney(String journeyId, String userId);
    // Lấy đối tượng entity Journey từ DB (Dùng nội bộ)
    Journey getJourneyEntity(String journeyId);
    
    // Lấy danh sách các yêu cầu xin tham gia hành trình
    List<JourneyRequestResponse> getPendingRequests(String journeyId, String userId);
    // Phê duyệt yêu cầu tham gia
    void approveRequest(String journeyId, String requestId, String ownerId);
    // Từ chối yêu cầu tham gia
    void rejectRequest(String journeyId, String requestId, String ownerId);
    
    // --- Các API dùng cho Profile (Hồ sơ người dùng) ---
    // Xem danh sách hành trình công khai của một người dùng
    List<UserActiveJourneyResponse> getUserPublicJourneys(String targetUserId, String currentUserId);
    // Xem danh sách hành trình riêng tư (nếu có quyền)
    List<UserActiveJourneyResponse> getUserPrivateJourneys(String targetUserId, String currentUserId);

    // --- API dùng cho Modal/Dashboard (Trang chủ) ---
    // Lấy danh sách các hành trình đang hoạt động (đang diễn ra) của người dùng
    List<UserActiveJourneyResponse> getUserActiveJourneys(String userId);

    // Lấy các cảnh báo liên quan đến hành trình (ví dụ: sắp hết hạn, cần checkin...)
    JourneyAlertResponse getJourneyAlerts(String userId);
    
    // Lấy danh sách bạn bè có thể mời tham gia hành trình này
    List<UserSummaryResponse> getInvitableFriends(String journeyId, String userId);
    
    // Bật/tắt việc hiển thị hành trình này trên trang cá nhân (Profile)
    void toggleProfileVisibility(String journeyId, String userId);
}