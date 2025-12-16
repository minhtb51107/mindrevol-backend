package com.mindrevol.backend.modules.user.dto.response;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDataExport {
    private UserProfileResponse profile;
    private List<HabitResponse> habits;
    private List<CheckinResponse> checkins;
    
    // [SỬA TẠI ĐÂY] Đổi từ UserSummaryResponse thành FriendshipResponse
    private List<FriendshipResponse> friends; 
    
    private List<String> badges;
}