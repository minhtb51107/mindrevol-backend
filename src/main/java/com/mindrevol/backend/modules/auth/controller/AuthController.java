package com.mindrevol.backend.modules.auth.controller;

import java.util.List;
import java.util.Map; // Import Map

import org.springframework.http.HttpStatus;
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
import com.mindrevol.backend.modules.auth.dto.request.AppleLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.FacebookLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.GoogleLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.LoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.LogoutRequest;
import com.mindrevol.backend.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.SendOtpRequest;
import com.mindrevol.backend.modules.auth.dto.request.TikTokLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.VerifyOtpRequest;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse; // Import mới

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // --- API MỚI: Check Email (cho luồng Spotify) ---
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> checkEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        UserSummaryResponse summary = authService.checkEmail(email);
        
        if (summary != null) {
            // Có user -> Trả về data (Avatar, Tên...)
            return ResponseEntity.ok(ApiResponse.success(summary, "Email đã tồn tại"));
        } else {
            // Không có user -> Trả về data null (Frontend sẽ hiểu là user mới)
            return ResponseEntity.ok(ApiResponse.success(null, "Email chưa đăng ký"));
        }
    }
    // ------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt."));
    }

    @GetMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@RequestParam("token") String token) {
        authService.activateUserAccount(token);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt tài khoản thành công!"));
    }

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

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        // Xử lý trường hợp client gửi kèm "Bearer " hoặc chỉ gửi token
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
    
    // --- Password Management ---

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

    // --- Session Management ---

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

    // --- Magic Link ---

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
}