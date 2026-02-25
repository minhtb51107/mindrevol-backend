package com.mindrevol.backend.modules.auth.service;

import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    // Login truyền thống
    JwtResponse login(LoginRequest request, HttpServletRequest servletRequest);

    // Login Social
    JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest);
    JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest);
    JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest);
    JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest);

    // Lấy thông tin bản thân (Có thể chuyển sang UserService, nhưng để tạm đây cũng được)
    UserProfileResponse getCurrentUserProfile(String userEmail);
    
    // Login bằng Magic Link (Link đăng nhập 1 lần gửi qua email)
    void sendMagicLink(String email);
    JwtResponse loginWithMagicLink(String token, HttpServletRequest request);

    // Login bằng OTP (Đăng nhập nhanh không cần pass)
    void sendOtpLogin(SendOtpRequest request);
    JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest);
}