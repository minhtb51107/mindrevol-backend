package com.mindrevol.backend.modules.auth.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.service.AuthService;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Auth: Login", description = "Các phương thức đăng nhập")
public class AuthController {

    private final AuthService authService;

    // --- TRADITIONAL LOGIN ---

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập thường", description = "Email & Password")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.login(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    // --- SOCIAL LOGIN ---

    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<JwtResponse>> loginGoogle(@RequestBody GoogleLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithGoogle(request, servletRequest)));
    }

    @PostMapping("/login/apple")
    public ResponseEntity<ApiResponse<JwtResponse>> loginApple(@RequestBody AppleLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithApple(request, servletRequest)));
    }

    @PostMapping("/login/facebook")
    public ResponseEntity<ApiResponse<JwtResponse>> loginFacebook(@RequestBody FacebookLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithFacebook(request, servletRequest)));
    }

    @PostMapping("/login/tiktok")
    public ResponseEntity<ApiResponse<JwtResponse>> loginTikTok(@RequestBody TikTokLoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.loginWithTikTok(request, servletRequest)));
    }

    // --- OTP & MAGIC LINK LOGIN ---

    @PostMapping("/otp/send")
    @Operation(summary = "Gửi OTP đăng nhập")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtpLogin(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã xác thực đã được gửi tới email."));
    }

    @PostMapping("/otp/login")
    @Operation(summary = "Đăng nhập bằng OTP")
    public ResponseEntity<ApiResponse<JwtResponse>> loginWithOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest servletRequest) {
        JwtResponse jwtResponse = authService.verifyOtpLogin(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    @PostMapping("/magic-link")
    @Operation(summary = "Gửi Magic Link")
    public ResponseEntity<ApiResponse<Void>> sendMagicLink(@RequestBody ForgotPasswordRequest request) {
        authService.sendMagicLink(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Magic link đã được gửi tới email."));
    }

    @PostMapping("/magic-login")
    @Operation(summary = "Đăng nhập Magic Link")
    public ResponseEntity<ApiResponse<JwtResponse>> loginWithMagicLink(@RequestParam("token") String token, HttpServletRequest request) {
        JwtResponse jwtResponse = authService.loginWithMagicLink(token, request);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse));
    }

    // --- PROFILE ---

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Lấy thông tin bản thân")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}