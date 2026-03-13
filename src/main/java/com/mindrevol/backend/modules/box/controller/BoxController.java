package com.mindrevol.backend.modules.box.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.backend.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.backend.modules.box.dto.response.BoxMemberResponse;
import com.mindrevol.backend.modules.box.dto.response.BoxResponse;
import com.mindrevol.backend.modules.box.dto.response.BoxInvitationResponse;
import com.mindrevol.backend.modules.box.entity.BoxInvitation;
import com.mindrevol.backend.modules.box.repository.BoxInvitationRepository;
import com.mindrevol.backend.modules.box.service.BoxService;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/boxes")
@RequiredArgsConstructor
public class BoxController {

    private final BoxService boxService;
    private final BoxInvitationRepository boxInvitationRepository; // Phục vụ riêng cho API lấy list thư mời

    @PostMapping
    public ResponseEntity<ApiResponse<BoxResponse>> createBox(@Valid @RequestBody CreateBoxRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(boxService.createBox(request, userId), "Tạo không gian thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoxResponse>> getBoxDetails(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(boxService.getBoxDetails(id, userId), "Lấy thông tin thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<BoxResponse>>> getMyBoxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(boxService.getMyBoxes(userId, PageRequest.of(page, size)), "Lấy danh sách không gian thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoxResponse>> updateBox(
            @PathVariable String id,
            @Valid @RequestBody UpdateBoxRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(boxService.updateBox(id, request, userId), "Cập nhật thành công"));
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
        return ResponseEntity.ok(ApiResponse.success(boxService.getBoxMembers(id, userId, PageRequest.of(page, size)), "Lấy danh sách thành viên thành công"));
    }

    @GetMapping("/{id}/journeys")
    public ResponseEntity<ApiResponse<Page<JourneyResponse>>> getBoxJourneys(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(boxService.getBoxJourneys(id, userId, PageRequest.of(page, size)), "Lấy danh sách hành trình thành công"));
    }

    // ==========================================
    // CÁC API QUẢN LÝ LỜI MỜI (ĐÃ CHUYỂN SANG DB)
    // ==========================================

    // [THÊM MỚI] Lấy danh sách lời mời đang chờ xử lý của User
    @GetMapping("/invitations/me")
    public ResponseEntity<ApiResponse<List<BoxInvitationResponse>>> getMyPendingInvitations() {
        String userId = SecurityUtils.getCurrentUserId();
        List<BoxInvitation> invitations = boxInvitationRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(userId, JourneyInvitationStatus.PENDING);
        
        List<BoxInvitationResponse> responseList = invitations.stream().map(inv -> BoxInvitationResponse.builder()
                .id(inv.getId())
                .boxId(inv.getBox().getId())
                .boxName(inv.getBox().getName())
                .boxAvatar(inv.getBox().getAvatar())
                .inviterId(inv.getInviter().getId())
                .inviterName(inv.getInviter().getFullname())
                .inviterAvatar(inv.getInviter().getAvatarUrl())
                .status(inv.getStatus().name())
                .sentAt(inv.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseList, "Lấy danh sách lời mời thành công"));
    }

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