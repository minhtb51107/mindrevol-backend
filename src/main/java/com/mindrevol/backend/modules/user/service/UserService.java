package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport; // <-- Import mới
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse; // Import DTO này
import com.mindrevol.backend.modules.user.entity.User;

import java.util.List;

public interface UserService {

    UserProfileResponse getMyProfile(String currentEmail);

    UserProfileResponse getPublicProfile(String handle, String currentUserEmail);

    UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request);
    
    void updateFcmToken(Long userId, String token);
    
    User getUserById(Long id);
    
    void deleteMyAccount(Long userId);

    // --- MỚI: Tải dữ liệu cá nhân ---
    UserDataExport exportMyData(Long userId);
    
    List<UserSummaryResponse> searchUsers(String query, Long currentUserId);
}