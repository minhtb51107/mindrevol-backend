package com.mindrevol.backend.modules.auth.service;

import java.util.List;

import com.mindrevol.backend.modules.auth.dto.request.AppleLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.FacebookLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.GoogleLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.LoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse; 
import com.mindrevol.backend.modules.auth.dto.request.SendOtpRequest;
import com.mindrevol.backend.modules.auth.dto.request.TikTokLoginRequest;
import com.mindrevol.backend.modules.auth.dto.request.VerifyOtpRequest; 

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    void registerUser(RegisterRequest request); 
    
    void activateUserAccount(String token);
    
    UserSummaryResponse checkEmail(String email);
    
    JwtResponse login(LoginRequest request, HttpServletRequest servletRequest);
    
    //JwtResponse loginWithGoogle(String idTokenString, HttpServletRequest servletRequest);
    
    JwtResponse refreshToken(String refreshToken);
    
    void logout(String refreshToken);
    
    void forgotPassword(ForgotPasswordRequest request);
    
    void resetPassword(ResetPasswordRequest request);
    
    void changePassword(ChangePasswordRequest request, String userEmail);
    
    UserProfileResponse getCurrentUserProfile(String userEmail); 
    
    List<UserSessionResponse> getAllSessions(String userEmail, String currentToken);
    
    void revokeSession(String sessionId, String userEmail);
    
    void sendMagicLink(String email);
    
    JwtResponse loginWithMagicLink(String token, HttpServletRequest request);
    
    JwtResponse loginWithApple(AppleLoginRequest request, HttpServletRequest servletRequest);
    
    void sendOtpLogin(SendOtpRequest request);
    
    JwtResponse verifyOtpLogin(VerifyOtpRequest request, HttpServletRequest servletRequest);
    
    JwtResponse loginWithFacebook(FacebookLoginRequest request, HttpServletRequest servletRequest);

	JwtResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest servletRequest);
	
	JwtResponse loginWithTikTok(TikTokLoginRequest request, HttpServletRequest servletRequest);
}