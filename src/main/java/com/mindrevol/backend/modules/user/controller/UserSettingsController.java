package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.backend.modules.user.entity.UserSettings;
import com.mindrevol.backend.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/settings")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "Cài đặt người dùng (Thông báo, Privacy...)")
public class UserSettingsController {

    private final UserService userService;

    // [UUID] SecurityUtils.getCurrentUserId() trả về String
    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy cài đặt thông báo của tôi")
    public ResponseEntity<ApiResponse<UserSettings>> getNotificationSettings() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getNotificationSettings(userId)));
    }

    @PutMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật cài đặt thông báo")
    public ResponseEntity<ApiResponse<UserSettings>> updateNotificationSettings(
            @RequestBody UpdateNotificationSettingsRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        UserSettings settings = userService.updateNotificationSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
}