package com.mindrevol.backend.modules.auth.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth: Password", description = "Quản lý mật khẩu và bảo mật")
public class PasswordController {

    private final PasswordService passwordService;

    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu", description = "Gửi link reset pass về email.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Link đặt lại mật khẩu đã được gửi tới email."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu", description = "Đổi mật khẩu từ token quên mật khẩu.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được thay đổi thành công."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Đổi mật khẩu", description = "Dành cho người dùng đã đăng nhập.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        passwordService.changePassword(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công."));
    }

    @GetMapping("/has-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Kiểm tra có mật khẩu chưa", description = "Dành cho user social.")
    public ResponseEntity<ApiResponse<Boolean>> checkHasPassword(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(passwordService.hasPassword(userDetails.getUsername())));
    }

    @PostMapping("/create-password")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Tạo mật khẩu mới", description = "Dành cho user social lần đầu đặt pass.")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @Valid @RequestBody CreatePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        passwordService.createPassword(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đã thiết lập mật khẩu thành công."));
    }

    @PostMapping("/update-password-otp")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cập nhật pass bằng OTP", description = "Bảo mật cao hơn cho việc đổi pass.")
    public ResponseEntity<ApiResponse<Void>> updatePasswordWithOtp(
            @Valid @RequestBody UpdatePasswordOtpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        passwordService.updatePasswordWithOtp(userDetails.getUsername(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mật khẩu thành công!"));
    }
}