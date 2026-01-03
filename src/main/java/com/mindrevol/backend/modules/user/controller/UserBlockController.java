package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.service.UserBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Block", description = "Quản lý chặn người dùng")
public class UserBlockController {

    private final UserBlockService userBlockService;

    // [UUID] @PathVariable String
    @PostMapping("/blocks/{targetId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chặn người dùng")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable String targetId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userBlockService.blockUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/blocks/{targetId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bỏ chặn người dùng")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable String targetId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userBlockService.unblockUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me/blocks")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách chặn")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> getBlockList() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userBlockService.getBlockList(userId)));
    }
}