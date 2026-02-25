package com.mindrevol.backend.modules.auth.service;

import com.mindrevol.backend.modules.auth.dto.response.JwtResponse;
import com.mindrevol.backend.modules.auth.dto.response.UserSessionResponse;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface SessionService {
    
    // Tạo token và lưu session mới (Dùng chung cho Login & Register)
    JwtResponse createTokenAndSession(User user, HttpServletRequest request);

    // Làm mới token khi hết hạn
    JwtResponse refreshToken(String refreshToken);

    // Đăng xuất (Xóa session)
    void logout(String refreshToken);

    // Lấy danh sách các thiết bị đang đăng nhập
    List<UserSessionResponse> getAllSessions(String userEmail, String currentTokenRaw);

    // Đăng xuất từ xa một thiết bị cụ thể
    void revokeSession(String sessionId, String userEmail);
}