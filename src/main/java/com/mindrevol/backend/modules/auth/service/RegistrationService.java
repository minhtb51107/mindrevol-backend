package com.mindrevol.backend.modules.auth.service;

import com.mindrevol.backend.modules.auth.dto.request.RegisterRequest;
import com.mindrevol.backend.modules.auth.dto.request.ResendRegisterOtpRequest;
import com.mindrevol.backend.modules.auth.dto.request.VerifyRegisterOtpRequest;
import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface RegistrationService {

    // Bước 1: Validate thông tin & Gửi OTP qua email (Lưu tạm vào Redis)
    void registerUserStep1(RegisterRequest request);

    // Bước 2: Xác thực OTP & Tạo User vào DB & Trả về Token
    JwtResponse verifyRegisterOtp(VerifyRegisterOtpRequest request, HttpServletRequest servletRequest);

    // Gửi lại mã OTP nếu chưa nhận được
    void resendRegisterOtp(ResendRegisterOtpRequest request);

    // Kiểm tra nhanh email đã tồn tại chưa (Dùng cho UI realtime)
    UserSummaryResponse checkEmail(String email);

    // Kiểm tra nhanh Handle (ID người dùng) đã tồn tại chưa
    boolean isHandleExists(String handle);
    
    // Kích hoạt tài khoản qua Link email (Legacy - Cách cũ)
    void activateUserAccount(String token);
}