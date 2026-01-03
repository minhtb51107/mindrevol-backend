package com.mindrevol.backend.modules.journey.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.request.InviteFriendRequest;
import com.mindrevol.backend.modules.journey.dto.request.JoinJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.request.UpdateJourneySettingsRequest;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    // [MỚI] API lấy thông tin chấm đỏ (Alerts)
    @GetMapping("/alerts")
    public ApiResponse<JourneyAlertResponse> getAlerts() {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getJourneyAlerts(userId));
    }
    
    @GetMapping("/users/{userId}/finished")
    @Operation(summary = "Lấy các hành trình đã kết thúc của user (cho Profile)")
    // 1. Đổi kiểu trả về thành ApiResponse
    public ApiResponse<List<UserActiveJourneyResponse>> getUserFinishedJourneys(@PathVariable String userId) {
        // 2. Dùng ApiResponse.success() để bọc dữ liệu
        return ApiResponse.success(journeyService.getUserFinishedJourneys(userId));
    }

    // [API CŨ] Lấy danh sách bạn bè mời (đã lọc)
    @GetMapping("/{id}/friends-invitable")
    public ApiResponse<List<UserSummaryResponse>> getInvitableFriends(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(journeyService.getInvitableFriends(id, userId));
    }

    // --- CÁC API CŨ GIỮ NGUYÊN (Create, Join, GetMe...) ---
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

    // [SỬA LỖI TẠI ĐÂY]
    // Thay đổi kiểu trả về từ ApiResponse<List<JourneyResponse>> 
    // thành ApiResponse<List<UserActiveJourneyResponse>>
    @GetMapping("/users/{userId}/active")
    public ApiResponse<List<UserActiveJourneyResponse>> getUserActiveJourneys(@PathVariable String userId) {
        if ("me".equalsIgnoreCase(userId)) {
            String currentUserId = SecurityUtils.getCurrentUserId();
            return ApiResponse.success(journeyService.getUserActiveJourneys(currentUserId));
        }
        return ApiResponse.success(journeyService.getUserActiveJourneys(userId));
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
}