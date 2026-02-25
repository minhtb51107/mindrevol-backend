package com.mindrevol.backend.modules.auth.service;

import com.mindrevol.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.CreatePasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mindrevol.backend.modules.auth.dto.request.ResetPasswordRequest;

public interface PasswordService {

    // Gửi email quên mật khẩu
    void forgotPassword(ForgotPasswordRequest request);

    // Đặt lại mật khẩu từ token quên mật khẩu
    void resetPassword(ResetPasswordRequest request);

    // Đổi mật khẩu (Khi đã đăng nhập)
    void changePassword(ChangePasswordRequest request, String userEmail);

    // Kiểm tra user có mật khẩu chưa (Dành cho user Social)
    boolean hasPassword(String email);

    // Tạo mật khẩu mới (Dành cho user Social lần đầu đặt pass)
    void createPassword(CreatePasswordRequest request, String email);

    // Cập nhật mật khẩu bằng OTP (Bảo mật cao hơn)
    void updatePasswordWithOtp(String email, String otpCode, String newPassword);
}