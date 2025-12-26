package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserSettings;

import java.util.List;

public interface UserService {

    UserProfileResponse getMyProfile(String currentEmail);

    UserProfileResponse getPublicProfile(String handle, String currentUserEmail);

    UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request);

    void updateFcmToken(Long userId, String token);
    
    User getUserById(Long id);

    void deleteMyAccount(Long userId);

    UserDataExport exportMyData(Long userId);

    List<UserSummaryResponse> searchUsers(String query, Long currentUserId);
    
    List<JourneyResponse> getUserRecaps(Long userId);

    // --- MỚI THÊM ---
    UserSettings getNotificationSettings(Long userId);
    
    UserSettings updateNotificationSettings(Long userId, UpdateNotificationSettingsRequest request);
    
    void createDefaultSettings(User user);
    
    List<LinkedAccountResponse> getLinkedAccounts(Long userId);
    
    void unlinkSocialAccount(Long userId, String provider);
}