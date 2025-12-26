package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import java.util.List;

public interface UserBlockService {
    void blockUser(Long userId, Long blockedId);
    void unblockUser(Long userId, Long blockedId);
    List<UserSummaryResponse> getBlockList(Long userId); // <--- Tên hàm phải là getBlockList
    boolean isBlocked(Long userId, Long targetUserId);
}