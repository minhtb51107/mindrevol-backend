package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils; // Thêm import
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        UserProfileResponse profile = userService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{handle}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getPublicProfile(
            @PathVariable String handle,
            Authentication authentication) {
        String currentEmail = (authentication != null && authentication.isAuthenticated()) 
                              ? authentication.getName() 
                              : null;
        UserProfileResponse profile = userService.getPublicProfile(handle, currentEmail);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updatedProfile = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile));
    }
    
    @PatchMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String token = body.get("token");
        userService.updateFcmToken(currentUser.getId(), token);
        return ResponseEntity.ok(ApiResponse.success("Updated FCM Token"));
    }

    // --- API MỚI: XÓA TÀI KHOẢN ---
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.deleteMyAccount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được xóa vĩnh viễn"));
    }
    // ------------------------------
}