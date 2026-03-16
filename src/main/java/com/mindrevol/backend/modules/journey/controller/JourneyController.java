package com.mindrevol.backend.modules.journey.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    @GetMapping("/alerts")
    public ApiResponse<JourneyAlertResponse> getAlerts() {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getJourneyAlerts(userId));
    }

    // --- API CHO MODAL/DASHBOARD ---
    @GetMapping("/users/{userId}/active")
    @Operation(summary = "Lấy toàn bộ hành trình đang hoạt động (cho Modal/Trang chủ)")
    public ApiResponse<List<UserActiveJourneyResponse>> getUserActiveJourneys(@PathVariable String userId) {
        if ("me".equalsIgnoreCase(userId)) {
            String currentUserId = SecurityUtils.getCurrentUserId();
            return ApiResponse.success(journeyService.getUserActiveJourneys(currentUserId));
        }
        return ApiResponse.success(journeyService.getUserActiveJourneys(userId));
    }

    // --- API CHO PROFILE THEO QUYỀN RIÊNG TƯ ---
    @GetMapping("/users/{userId}/public")
    @Operation(summary = "Lấy các hành trình CÔNG KHAI của user (cho Profile)")
    public ApiResponse<List<UserActiveJourneyResponse>> getUserPublicJourneys(@PathVariable String userId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        String targetId = "me".equalsIgnoreCase(userId) ? currentUserId : userId;
        return ApiResponse.success(journeyService.getUserPublicJourneys(targetId, currentUserId));
    }

    @GetMapping("/users/{userId}/private")
    @Operation(summary = "Lấy các hành trình RIÊNG TƯ của user (cho Profile)")
    public ApiResponse<List<UserActiveJourneyResponse>> getUserPrivateJourneys(@PathVariable String userId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        String targetId = "me".equalsIgnoreCase(userId) ? currentUserId : userId;
        return ApiResponse.success(journeyService.getUserPrivateJourneys(targetId, currentUserId));
    }
    // --------------------------------------------------

    @GetMapping("/{id}/friends-invitable")
    public ApiResponse<List<UserSummaryResponse>> getInvitableFriends(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getInvitableFriends(id, userId));
    }

    @PostMapping
    public ApiResponse<JourneyResponse> createJourney(@Valid @RequestBody CreateJourneyRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.createJourney(request, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<JourneyResponse> getJourney(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getJourneyDetail(userId, id));
    }

    @PostMapping("/join/{inviteCode}")
    public ApiResponse<JourneyResponse> joinJourney(@PathVariable String inviteCode) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.joinJourney(inviteCode, userId));
    }

    @GetMapping("/me")
    public ApiResponse<List<JourneyResponse>> getMyJourneys() {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getMyJourneys(userId));
    }

    @GetMapping("/{id}/requests/pending")
    public ApiResponse<List<JourneyRequestResponse>> getPendingRequests(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getPendingRequests(id, userId));
    }

    @PostMapping("/{journeyId}/requests/{requestId}/approve")
    public ApiResponse<Void> approveRequest(@PathVariable String journeyId, @PathVariable String requestId) {
        String userId = SecurityUtils.getCurrentUserId();
        journeyService.approveRequest(journeyId, requestId, userId);
        return ApiResponse.success(null, "Đã duyệt thành viên");
    }

    @PostMapping("/{journeyId}/requests/{requestId}/reject")
    public ApiResponse<Void> rejectRequest(@PathVariable String journeyId, @PathVariable String requestId) {
        String userId = SecurityUtils.getCurrentUserId();
        journeyService.rejectRequest(journeyId, requestId, userId);
        return ApiResponse.success(null, "Đã từ chối yêu cầu");
    }

    @PatchMapping("/{id}/settings")
    public ApiResponse<JourneyResponse> updateJourneySettings(@PathVariable String id, @RequestBody CreateJourneyRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.updateJourney(id, request, userId));
    }

    @DeleteMapping("/{id}/leave")
    public ApiResponse<Void> leaveJourney(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        journeyService.leaveJourney(id, userId);
        return ApiResponse.success(null, "Đã rời hành trình");
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ApiResponse<Void> kickMember(@PathVariable String id, @PathVariable String memberId) {
        String userId = SecurityUtils.getCurrentUserId();
        journeyService.kickMember(id, memberId, userId);
        return ApiResponse.success(null, "Đã mời thành viên ra khỏi nhóm");
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<List<JourneyParticipantResponse>> getParticipants(@PathVariable String id) {
        return ApiResponse.success(journeyService.getJourneyParticipants(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteJourney(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        journeyService.deleteJourney(id, userId);
        return ApiResponse.success(null, "Đã giải tán hành trình");
    }
    
    @PostMapping("/{id}/transfer-ownership")
    public ApiResponse<Void> transferOwnership(@PathVariable String id, @RequestParam String newOwnerId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        journeyService.transferOwnership(id, currentUserId, newOwnerId);
        return ApiResponse.success(null, "Đã chuyển quyền sở hữu");
    }
    
 // ... Thêm cục này vào bên trong Controller
    @PatchMapping("/{id}/profile-visibility")
    @Operation(summary = "Bật/Tắt hiển thị Hành trình trên Trang cá nhân")
    public ApiResponse<Void> toggleProfileVisibility(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        journeyService.toggleProfileVisibility(id, userId);
        return ApiResponse.success(null, "Đã cập nhật trạng thái hiển thị trên Trang cá nhân");
    }
// ...
}