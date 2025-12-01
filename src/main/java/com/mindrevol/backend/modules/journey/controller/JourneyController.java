package com.mindrevol.backend.modules.journey.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.request.CreateJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.request.JoinJourneyRequest;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyWidgetResponse;
import com.mindrevol.backend.modules.journey.dto.response.RoadmapStatusResponse;
import com.mindrevol.backend.modules.journey.service.JourneyService;
import com.mindrevol.backend.modules.user.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    // API 1: Tạo hành trình mới
    @PostMapping
    public ResponseEntity<ApiResponse<JourneyResponse>> createJourney(
            @Valid @RequestBody CreateJourneyRequest request,
            @AuthenticationPrincipal User currentUser) { // Lấy user từ Token
        
        JourneyResponse response = journeyService.createJourney(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo hành trình thành công"));
    }

    // API 2: Tham gia hành trình bằng mã
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JourneyResponse>> joinJourney(
            @Valid @RequestBody JoinJourneyRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        JourneyResponse response = journeyService.joinJourney(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Tham gia thành công"));
    }

    // API 3: Lấy danh sách hành trình của tôi (Trang chủ)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<JourneyResponse>>> getMyJourneys(
            @AuthenticationPrincipal User currentUser) {
        
        List<JourneyResponse> response = journeyService.getMyJourneys(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách thành công"));
    }
    
    @DeleteMapping("/{journeyId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @PathVariable UUID journeyId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal User currentUser) {
        
        journeyService.kickMember(journeyId, memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Đã mời thành viên ra khỏi nhóm"));
    }
    
    @GetMapping("/{id}/widget-info")
    public ApiResponse<JourneyWidgetResponse> getWidgetInfo(@PathVariable UUID id) {
        Long currentUserId = SecurityUtils.getCurrentUserId(); 
        
        return ApiResponse.success(journeyService.getWidgetInfo(id, currentUserId), "Lấy thông tin Widget thành công");
    }
}