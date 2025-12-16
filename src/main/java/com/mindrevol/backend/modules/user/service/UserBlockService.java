package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import java.util.List;

public interface UserBlockService {
    void blockUser(Long userId, Long blockId);
    void unblockUser(Long userId, Long blockId);
    List<UserSummaryResponse> getBlockedUsers(Long userId);
    
    // [THÊM MỚI] Kiểm tra xem user A có chặn user B không
    boolean isBlocked(Long userId, Long targetUserId);
}