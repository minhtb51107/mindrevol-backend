package com.mindrevol.backend.modules.journey.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.request.*;
import com.mindrevol.backend.modules.journey.dto.response.*;
import com.mindrevol.backend.modules.journey.service.JourneyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.mindrevol.backend.modules.user.entity.User;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<JourneyResponse>> createJourney(@Valid @RequestBody CreateJourneyRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        JourneyResponse response = journeyService.createJourney(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Tạo hành trình thành công"));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JourneyResponse>> joinJourney(@Valid @RequestBody JoinJourneyRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        JourneyResponse response = journeyService.joinJourney(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tham gia thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<JourneyResponse>>> getMyJourneys() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<JourneyResponse> response = journeyService.getMyJourneys(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách thành công"));
    }

    // --- TAB 1: Danh sách hành trình đang hoạt động ---
    @GetMapping("/users/{userId}/active")
    public ResponseEntity<ApiResponse<List<UserActiveJourneyResponse>>> getUserActiveJourneys(
            @PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        List<UserActiveJourneyResponse> response = journeyService.getUserActiveJourneys(userId, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách hành trình đang hoạt động thành công"));
    }

    // --- TAB 2: Danh sách hành trình đã kết thúc ---
    @GetMapping("/users/{userId}/finished")
    public ResponseEntity<ApiResponse<List<UserActiveJourneyResponse>>> getUserFinishedJourneys(
            @PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        List<UserActiveJourneyResponse> response = journeyService.getUserFinishedJourneys(userId, currentUserId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách hành trình đã kết thúc thành công"));
    }
    // -----------------------------------------------------

    @DeleteMapping("/{journeyId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveJourney(@PathVariable UUID journeyId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.leaveJourney(journeyId, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã rời khỏi hành trình"));
    }

    @PatchMapping("/{journeyId}/settings")
    public ResponseEntity<ApiResponse<JourneyResponse>> updateSettings(
            @PathVariable UUID journeyId, @RequestBody UpdateJourneySettingsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        JourneyResponse response = journeyService.updateJourneySettings(journeyId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật cài đặt thành công"));
    }

    @DeleteMapping("/{journeyId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(@PathVariable UUID journeyId, @PathVariable Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.kickMember(journeyId, memberId, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã mời thành viên ra khỏi nhóm"));
    }
    
    @GetMapping("/{id}/widget-info")
    public ApiResponse<JourneyWidgetResponse> getWidgetInfo(@PathVariable UUID id) {
        Long currentUserId = SecurityUtils.getCurrentUserId(); 
        return ApiResponse.success(journeyService.getWidgetInfo(id, currentUserId), "Lấy thông tin Widget thành công");
    }
    
    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRequest(@PathVariable UUID requestId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.approveJoinRequest(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã duyệt thành viên"));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(@PathVariable UUID requestId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.rejectJoinRequest(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã từ chối yêu cầu"));
    }

    @GetMapping("/discovery")
    public ResponseEntity<ApiResponse<List<JourneyResponse>>> getDiscoveryTemplates() {
        return ResponseEntity.ok(ApiResponse.success(journeyService.getDiscoveryTemplates()));
    }

    @PostMapping("/{journeyId}/fork")
    public ResponseEntity<ApiResponse<JourneyResponse>> forkJourney(@PathVariable UUID journeyId) {
        Long userId = SecurityUtils.getCurrentUserId();
        JourneyResponse response = journeyService.forkJourney(journeyId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Đã sao chép hành trình thành công"));
    }

    @PostMapping("/{journeyId}/members/{memberId}/nudge")
    public ResponseEntity<ApiResponse<Void>> nudgeMember(@PathVariable UUID journeyId, @PathVariable Long memberId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.nudgeMember(journeyId, memberId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã chọc ghẹo thành công!"));
    }
    
    @GetMapping("/{journeyId}/participants")
    public ResponseEntity<ApiResponse<List<JourneyParticipantResponse>>> getParticipants(@PathVariable UUID journeyId) {
        return ResponseEntity.ok(ApiResponse.success(journeyService.getJourneyParticipants(journeyId)));
    }
    
    @PostMapping("/{id}/transfer-ownership")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable UUID id,
            @RequestParam Long newOwnerId,
            @AuthenticationPrincipal User currentUser) { 
        journeyService.transferOwnership(id, currentUser.getId(), newOwnerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Chuyển quyền sở hữu thành công"));
    }
    
    @DeleteMapping("/{journeyId}")
    public ResponseEntity<ApiResponse<Void>> deleteJourney(@PathVariable UUID journeyId) {
        Long userId = SecurityUtils.getCurrentUserId();
        journeyService.deleteJourney(journeyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã giải tán hành trình thành công"));
    }
    
    @GetMapping("/{journeyId}/requests/pending")
    public ResponseEntity<ApiResponse<List<JourneyRequestResponse>>> getPendingRequests(@PathVariable UUID journeyId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(journeyService.getPendingRequests(journeyId, userId)));
    }
}