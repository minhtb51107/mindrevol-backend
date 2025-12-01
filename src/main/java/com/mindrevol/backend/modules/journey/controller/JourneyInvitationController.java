package com.mindrevol.backend.modules.journey.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.request.InviteFriendRequest;
import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.service.JourneyInvitationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/journey-invitations")
@RequiredArgsConstructor
public class JourneyInvitationController {

    private final JourneyInvitationService invitationService;
    private final UserService userService;

    // 1. Mời bạn bè
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> inviteFriend(@Valid @RequestBody InviteFriendRequest request) {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        
        invitationService.inviteFriendToJourney(currentUser, request.getJourneyId(), request.getFriendId());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 2. Chấp nhận lời mời
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long invitationId) {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        
        invitationService.acceptInvitation(currentUser, invitationId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 3. Từ chối lời mời
    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(@PathVariable Long invitationId) {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        
        invitationService.rejectInvitation(currentUser, invitationId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 4. Xem danh sách lời mời đang chờ
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<JourneyInvitationResponse>>> getMyInvitations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        Page<JourneyInvitationResponse> invitations = invitationService.getMyPendingInvitations(currentUser, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }
}