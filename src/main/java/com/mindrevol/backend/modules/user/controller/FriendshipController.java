package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.user.dto.request.FriendRequestAction;
import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.FriendshipService;
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
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserService userService; // Để lấy currentUser

    // 1. Gửi lời mời kết bạn
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(
            @Valid @RequestBody FriendRequestAction request) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipResponse response = friendshipService.sendFriendRequest(currentUserId, request.getTargetUserId());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 2. Chấp nhận lời mời (truyền ID của Friendship, không phải ID user)
    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(
            @PathVariable Long friendshipId) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        FriendshipResponse response = friendshipService.acceptFriendRequest(currentUserId, friendshipId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 3. Từ chối lời mời
    @PostMapping("/decline/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> declineRequest(
            @PathVariable Long friendshipId) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        friendshipService.declineFriendRequest(currentUserId, friendshipId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 4. Hủy kết bạn (Unfriend)
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> unfriend(
            @PathVariable Long targetUserId) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        friendshipService.removeFriendship(currentUserId, targetUserId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 5. Lấy danh sách bạn bè
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getMyFriends(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<FriendshipResponse> friends = friendshipService.getMyFriends(currentUserId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(friends));
    }

    // 6. Lấy danh sách lời mời đang chờ tôi duyệt (Incoming)
    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getIncomingRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<FriendshipResponse> requests = friendshipService.getIncomingRequests(currentUserId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
}