package com.mindrevol.backend.modules.user.dto.response;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
// Đã xóa import HabitResponse
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDataExport {
    private UserProfileResponse profile;
    // Đã xóa habits
    private List<CheckinResponse> checkins;
    private List<FriendshipResponse> friends; 
    // Đã xóa badges
}