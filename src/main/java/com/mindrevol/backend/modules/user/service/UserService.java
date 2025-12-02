package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.entity.User;

public interface UserService {

    UserProfileResponse getMyProfile(String currentEmail);

    UserProfileResponse getPublicProfile(String handle, String currentUserEmail);

    UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request);
    
    void updateFcmToken(Long userId, String token);
    
    User getUserById(Long id);
    
    // --- THÊM HÀM NÀY ---
    void deleteMyAccount(Long userId);
}