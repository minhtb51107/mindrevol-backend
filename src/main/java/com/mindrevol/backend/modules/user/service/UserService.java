package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserSettings;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import org.springframework.web.multipart.MultipartFile; // Import này

import java.util.List;

public interface UserService {

    UserProfileResponse getMyProfile(String currentEmail);

    UserProfileResponse getPublicProfile(String handle, String currentUserEmail);
    
    UserProfileResponse getPublicProfileById(String userId, String currentUserEmail);

    // [CẬP NHẬT] Thêm MultipartFile vào tham số
    UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request, MultipartFile file);

    void updateFcmToken(String userId, String token);

    User getUserById(String id);

    void deleteMyAccount(String userId);
    
    UserDataExport exportMyData(String userId);
    
    List<UserSummaryResponse> searchUsers(String query, String currentUserId);
    
    List<JourneyResponse> getUserRecaps(String userId);

    UserSettings getNotificationSettings(String userId);

    UserSettings updateNotificationSettings(String userId, UpdateNotificationSettingsRequest request);

    void createDefaultSettings(User user);

    List<LinkedAccountResponse> getLinkedAccounts(String userId);

    void unlinkSocialAccount(String userId, String provider);
}