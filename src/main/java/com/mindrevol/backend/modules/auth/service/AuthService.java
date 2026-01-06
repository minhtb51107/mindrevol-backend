package com.mindrevol.backend.modules.auth.service;

import com.mindrevol.backend.modules.auth.dto.request.*;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AuthService {
    
    void registerUser(RegisterRequest request);
    
    void activateUserAccount(String token);
    
    JwtResponse login(LoginRequest request, HttpServletRequest servletRequest);
    
    JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest);
    
    JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest);
    
    JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest);
    
    JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest);
    
    JwtResponse refreshToken(String refreshToken);
    
    void logout(String refreshToken);
    
    UserProfileResponse getCurrentUserProfile(String userEmail);
    
    void forgotPassword(ForgotPasswordRequest request);
    
    void resetPassword(ResetPasswordRequest request);
    
    void changePassword(ChangePasswordRequest request, String userEmail);
    
    List<UserSessionResponse> getAllSessions(String userEmail, String currentTokenRaw);
    
    void revokeSession(String sessionId, String userEmail);
    
    void sendMagicLink(String email);
    
    JwtResponse loginWithMagicLink(String token, HttpServletRequest request);
    
    void sendOtpLogin(SendOtpRequest request);
    
    JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest);
    
    UserSummaryResponse checkEmail(String email);

    // --- MỚI THÊM ---
    boolean hasPassword(String email);
    
    void createPassword(CreatePasswordRequest request, String email);

	void updatePasswordWithOtp(String email, String otpCode, String newPassword);
}