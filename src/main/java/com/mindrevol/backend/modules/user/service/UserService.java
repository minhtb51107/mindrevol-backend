package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.request.UpdateNotificationSettingsRequest;
import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.LinkedAccountResponse;
import com.mindrevol.backend.modules.user.dto.response.UserDataExport;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.entity.UserSettings;
import com.mindrevol.backend.modules.checkin.dto.response.CalendarRecapResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import org.springframework.web.multipart.MultipartFile; 

import java.util.List;

/**
 * Service quản lý tài khoản, hồ sơ và cài đặt của người dùng.
 */
public interface UserService {

    // Lấy hồ sơ cá nhân của chính mình
    UserProfileResponse getMyProfile(String currentEmail);

    // Xem hồ sơ công khai của người khác thông qua "handle" (username định danh)
    UserProfileResponse getPublicProfile(String handle, String currentUserEmail);
    
    // Xem hồ sơ công khai của người khác thông qua ID
    UserProfileResponse getPublicProfileById(String userId, String currentUserEmail);

    // Cập nhật thông tin hồ sơ (có hỗ trợ upload avatar dạng MultipartFile)
    UserProfileResponse updateProfile(String currentEmail, UpdateProfileRequest request, MultipartFile file);

    // Cập nhật FCM Token (dùng cho Push Notification)
    void updateFcmToken(String userId, String token);

    // Tìm entity User theo ID (Dùng nội bộ)
    User getUserById(String id);

    // Yêu cầu xóa tài khoản cá nhân
    void deleteMyAccount(String userId);
    
    // Trích xuất toàn bộ dữ liệu người dùng (để tuân thủ quyền riêng tư/GDPR)
    UserDataExport exportMyData(String userId);
    
    // Tìm kiếm người dùng khác trong hệ thống
    List<UserSummaryResponse> searchUsers(String query, String currentUserId);
    
    // Lấy danh sách video/hình ảnh Recap kỷ niệm của người dùng
    List<JourneyResponse> getUserRecaps(String userId);

    // Lấy cài đặt thông báo của người dùng
    UserSettings getNotificationSettings(String userId);

    // Cập nhật cài đặt thông báo
    UserSettings updateNotificationSettings(String userId, UpdateNotificationSettingsRequest request);

    // Khởi tạo các cài đặt mặc định khi người dùng mới đăng ký
    void createDefaultSettings(User user);

    // Lấy danh sách các tài khoản MXH đã liên kết (Google, Apple, Facebook...)
    List<LinkedAccountResponse> getLinkedAccounts(String userId);

    // Hủy liên kết một tài khoản MXH
    void unlinkSocialAccount(String userId, String provider);

    // Lấy dữ liệu tóm tắt (Recap) hiển thị trên Lịch của người dùng theo Tháng/Năm
	List<CalendarRecapResponse> getUserCalendarRecap(String userId, int year, int month);
}