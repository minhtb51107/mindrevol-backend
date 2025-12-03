package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userBlockService.blockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã chặn người dùng"));
    }

    @DeleteMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userBlockService.unblockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã bỏ chặn người dùng"));
    }
}