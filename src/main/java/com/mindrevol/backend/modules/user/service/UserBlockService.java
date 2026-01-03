package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import java.util.List;

public interface UserBlockService {
    // [UUID] Đổi Long -> String
    void blockUser(String userId, String blockedId);
    void unblockUser(String userId, String blockedId);
    List<UserSummaryResponse> getBlockList(String userId);
    boolean isBlocked(String userId, String targetUserId);
}