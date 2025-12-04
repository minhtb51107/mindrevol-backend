package com.mindrevol.backend.modules.checkin.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.VoteRequest;
import com.mindrevol.backend.modules.checkin.service.VerificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final UserService userService;

    // API để User vote cho một bài Check-in
    // Endpoint: POST /api/v1/verifications/{checkinId}/vote
    // Body: { "isApproved": false } -> Báo cáo bài này là Fake
    @PostMapping("/{checkinId}/vote")
    public ResponseEntity<ApiResponse<Void>> castVote(
            @PathVariable UUID checkinId,
            @Valid @RequestBody VoteRequest request) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // Lấy User Entity đầy đủ để truyền xuống Service
        User currentUser = userService.getUserById(currentUserId);
        
        verificationService.castVote(checkinId, currentUser, request.getIsApproved());
        
        return ResponseEntity.ok(ApiResponse.success(null, "Đã gửi phiếu bầu thành công"));
    }
}