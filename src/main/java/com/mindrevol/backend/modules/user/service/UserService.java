package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.User;

public interface UserService {

    /**
     * Lấy thông tin profile đầy đủ của người dùng hiện tại.
     */
    UserProfileResponse getMyProfile(String currentEmail);

    /**
     * Lấy thông tin profile công khai của một user khác thông qua handle.
     * @param handle Handle (username) của người cần xem.
     * @param currentUserEmail Email người xem (có thể null nếu khách vãng lai).
     */
    UserProfileResponse getPublicProfile(String handle, String currentUserEmail);

    /**
     * Cập nhật thông tin cá nhân.
     * Dữ liệu đầu vào sẽ được sanitize (lọc XSS) trước khi lưu.
     */
    UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request);
    
    void updateFcmToken(Long userId, String token);
    
    User getUserById(Long id);
}