package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.user.dto.request.FriendRequestAction;
import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.backend.modules.user.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(
            @Valid @RequestBody FriendRequestAction request) {
        
    	String currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipResponse response = friendshipService.sendFriendRequest(currentUserId, request.getTargetUserId());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(
            @PathVariable String friendshipId) {
        
    	String currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipResponse response = friendshipService.acceptFriendRequest(currentUserId, friendshipId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/decline/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> declineRequest(
            @PathVariable String friendshipId) {
        
    	String currentUserId = SecurityUtils.getCurrentUserId();
        friendshipService.declineFriendRequest(currentUserId, friendshipId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unfriend(
            @PathVariable String targetUserId) {
        
    	String currentUserId = SecurityUtils.getCurrentUserId();
        friendshipService.removeFriendship(currentUserId, targetUserId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getMyFriends(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
    	String currentUserId = SecurityUtils.getCurrentUserId();
        Page<FriendshipResponse> friends = friendshipService.getMyFriends(currentUserId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(friends));
    }

    // [MỚI] Endpoint lấy danh sách bạn bè của người khác
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getUserFriends(
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<FriendshipResponse> friends = friendshipService.getUserFriends(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(friends));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getIncomingRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
    	String currentUserId = SecurityUtils.getCurrentUserId();
        Page<FriendshipResponse> requests = friendshipService.getIncomingRequests(currentUserId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
}