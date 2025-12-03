package com.mindrevol.backend.modules.user.dto.response;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDataExport {
    // 1. Thông tin cá nhân
    private UserProfileResponse profile;
    
    // 2. Danh sách thói quen
    private List<HabitResponse> habits;
    
    // 3. Lịch sử bài đăng (Check-in)
    private List<CheckinResponse> checkins;
    
    // 4. Danh sách bạn bè
    private List<UserSummaryResponse> friends;
    
    // 5. Huy hiệu đã đạt
    private List<String> badges; // Lưu tên badge cho gọn
}