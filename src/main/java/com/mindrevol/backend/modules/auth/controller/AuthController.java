package com.mindrevol.backend.modules.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================================================================
    // 1. VALIDATION ENDPOINTS (Dùng cho Async Check ở Frontend)
    // ==================================================================

    @PostMapping("/check-email")
    @Operation(summary = "Kiểm tra email đã tồn tại chưa")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        UserSummaryResponse summary = authService.checkEmail(request.getEmail());
        if (summary != null) {
            return ResponseEntity.ok(ApiResponse.success(summary, "Email đã tồn tại"));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null, "Email hợp lệ"));
        }
    }

    @PostMapping("/check-handle")
    @Operation(summary = "Kiểm tra handle (username) đã tồn tại chưa")
    public ResponseEntity<ApiResponse<Boolean>> checkHandle(@Valid @RequestBody CheckHandleRequest request) {
        boolean exists = authService.isHandleExists(request.getHandle());
        if (exists) {
            return ResponseEntity.ok(ApiResponse.success(true, "Handle đã được sử dụng"));
        } else {
            return ResponseEntity.ok(ApiResponse.success(false, "Handle hợp lệ"));
        }
    }

    // ==================================================================
    // 2. NEW REGISTRATION FLOW (Redis -> OTP -> DB)
    // ==================================================================

    @PostMapping("/register")
    @Operation(summary = "Bước 1: Gửi thông tin -> Nhận OTP (Chưa tạo User)")
    public ResponseEntity<ApiResponse<Void>> registerStep1(@Valid @RequestBody RegisterRequest request) {
        authService.registerUserStep1(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực (OTP) đã được gửi đến email của bạn."));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Bước 2: Xác thực OTP -> Tạo User -> Trả về Token Login")
    public ResponseEntity<ApiResponse<JwtResponse>> verifyRegisterOtp(
            @Valid @RequestBody VerifyRegisterOtpRequest request,
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.verifyRegisterOtp(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "Đăng ký thành công!"));
    }

    @PostMapping("/register/resend")
    @Operation(summary = "Gửi lại mã OTP đăng ký (nếu hết hạn/không nhận được)")
    public ResponseEntity<ApiResponse<Void>> resendRegisterOtp(@Valid @RequestBody ResendRegisterOtpRequest request) {
        authService.resendRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực mới đã được gửi."));
    }

    // ==================================================================
    // 3. LOGIN & SOCIAL AUTH
    // ==================================================================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.login(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<JwtResponse>> loginGoogle(@RequestBody GoogleLoginRequest request, HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.loginWithGoogle(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/login/apple")
    public ResponseEntity<ApiResponse<JwtResponse>> loginApple(@RequestBody AppleLoginRequest request, HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.loginWithApple(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/login/facebook")
    public ResponseEntity<ApiResponse<JwtResponse>> loginFacebook(
            @RequestBody FacebookLoginRequest request, 
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.loginWithFacebook(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }
    
    @PostMapping("/login/tiktok")
    public ResponseEntity<ApiResponse<JwtResponse>> loginTikTok(
            @RequestBody TikTokLoginRequest request, 
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.loginWithTikTok(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    // ==================================================================
    // 4. SESSION & TOKEN MANAGEMENT
    // ==================================================================

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;
        JwtResponse jwtResponse = authService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công."));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> getSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String currentToken) {
        List<UserSessionResponse> sessions = authService.getAllSessions(userDetails.getUsername(), currentToken);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.revokeSession(sessionId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đã đăng xuất thiết bị thành công."));
    }

    // ==================================================================
    // 5. PASSWORD MANAGEMENT
    // ==================================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Link đặt lại mật khẩu đã được gửi tới email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được thay đổi thành công."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.changePassword(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công."));
    }

    @GetMapping("/has-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> checkHasPassword(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(authService.hasPassword(userDetails.getUsername())));
    }

    @PostMapping("/create-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @Valid @RequestBody CreatePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.createPassword(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đã thiết lập mật khẩu thành công."));
    }
    
    @PostMapping("/update-password-otp")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<ApiResponse<Void>> updatePasswordWithOtp(
            @Valid @RequestBody UpdatePasswordOtpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.updatePasswordWithOtp(userDetails.getUsername(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mật khẩu thành công!"));
    }

    // ==================================================================
    // 6. OTP LOGIN & MAGIC LINK (Cho người dùng cũ/quên pass)
    // ==================================================================

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtpLogin(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực đã được gửi tới email."));
    }
    
    @PostMapping("/otp/login")
    public ResponseEntity<ApiResponse<JwtResponse>> loginWithOtp(
            @Valid @RequestBody VerifyOtpRequest request, 
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.verifyOtpLogin(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/magic-link")
    public ResponseEntity<ApiResponse<Void>> sendMagicLink(@RequestBody ForgotPasswordRequest request) {
        authService.sendMagicLink(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Magic link đã được gửi tới email."));
    }

    @PostMapping("/magic-login")
    public ResponseEntity<ApiResponse<JwtResponse>> loginWithMagicLink(@RequestParam("token") String token, HttpServletRequest request) {
        JwtResponse jwtResponse = authService.loginWithMagicLink(token, request);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }
    
    // API kích hoạt cũ (Có thể giữ lại để tương thích ngược nếu cần, hoặc xóa đi)
    @GetMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam("token") String token) {
        authService.activateUserAccount(token);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt tài khoản thành công!"));
    }
}