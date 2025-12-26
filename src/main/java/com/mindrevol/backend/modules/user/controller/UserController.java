package com.mindrevol.backend.modules.user.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
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

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.deleteMyAccount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được xóa vĩnh viễn"));
    }
    
    @GetMapping("/me/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDataExport>> exportData() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserDataExport data = userService.exportMyData(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> searchUsers(@RequestParam String query) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<UserSummaryResponse> results = userService.searchUsers(query, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
    
    @GetMapping("/{userId}/recaps")
    @Operation(summary = "Lấy danh sách các hành trình đã hoàn thành (Album kỷ niệm)")
    public ResponseEntity<ApiResponse<List<JourneyResponse>>> getUserRecaps(@PathVariable Long userId) {
        List<JourneyResponse> recaps = userService.getUserRecaps(userId);
        return ResponseEntity.ok(ApiResponse.success(recaps));
    }

    // --- CÁC API MỚI: QUẢN LÝ LIÊN KẾT MXH ---

    @GetMapping("/me/social-accounts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem danh sách tài khoản MXH đã liên kết")
    public ResponseEntity<ApiResponse<List<LinkedAccountResponse>>> getLinkedAccounts() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getLinkedAccounts(userId)));
    }

    @DeleteMapping("/me/social-accounts/{provider}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy liên kết tài khoản MXH")
    public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(@PathVariable String provider) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.unlinkSocialAccount(userId, provider.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy liên kết tài khoản " + provider));
    }
}