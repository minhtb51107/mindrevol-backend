package com.mindrevol.backend.modules.box.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.backend.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.backend.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.backend.modules.box.dto.response.BoxResponse;
import com.mindrevol.backend.modules.box.service.BoxService;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/boxes")
@RequiredArgsConstructor
public class BoxController {

    private final BoxService boxService;

    @PostMapping
    public ResponseEntity<ApiResponse<BoxResponse>> createBox(@Valid @RequestBody CreateBoxRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        BoxResponse response = boxService.createBox(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo không gian thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoxResponse>> getBoxDetails(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        BoxResponse response = boxService.getBoxDetails(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<BoxResponse>>> getMyBoxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        Page<BoxResponse> response = boxService.getMyBoxes(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách không gian thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoxResponse>> updateBox(
            @PathVariable String id,
            @Valid @RequestBody UpdateBoxRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        BoxResponse response = boxService.updateBox(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thành công"));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<String>> archiveBox(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        boxService.archiveBox(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa không gian này vào lưu trữ"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> disbandBox(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        boxService.disbandBox(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã giải tán không gian thành công"));
    }

    @PutMapping("/{id}/transfer-ownership/{newOwnerId}")
    public ResponseEntity<ApiResponse<String>> transferOwnership(
            @PathVariable String id,
            @PathVariable String newOwnerId) {
        String requesterId = SecurityUtils.getCurrentUserId();
        boxService.transferOwnership(id, newOwnerId, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển quyền chủ phòng thành công"));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Page<BoxMemberResponse>>> getBoxMembers(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        Page<BoxMemberResponse> response = boxService.getBoxMembers(id, userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách thành viên thành công"));
    }

    @GetMapping("/{id}/journeys")
    public ResponseEntity<ApiResponse<Page<JourneyResponse>>> getBoxJourneys(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        Page<JourneyResponse> response = boxService.getBoxJourneys(id, userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách hành trình thành công"));
    }

    // ==========================================
    // CÁC API QUẢN LÝ THÀNH VIÊN (ĐÃ SỬA)
    // ==========================================

    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<String>> inviteMember(
            @PathVariable String id,
            @RequestParam String targetUserId) {
        String requesterId = SecurityUtils.getCurrentUserId();
        boxService.inviteMember(id, targetUserId, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi lời mời tham gia không gian"));
    }

    @PostMapping("/{id}/accept-invite")
    public ResponseEntity<ApiResponse<String>> acceptInvite(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        boxService.acceptInvite(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã tham gia không gian"));
    }

    @PostMapping("/{id}/reject-invite")
    public ResponseEntity<ApiResponse<String>> rejectInvite(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        boxService.rejectInvite(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối lời mời"));
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @PathVariable String id,
            @PathVariable String targetUserId) {
        String requesterId = SecurityUtils.getCurrentUserId();
        boxService.removeMember(id, targetUserId, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thành viên khỏi không gian"));
    }
}