package com.mindrevol.backend.modules.user.service;

import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import java.util.List;

/**
 * Service quản lý tính năng chặn (Block) người dùng.
 */
public interface UserBlockService {
    // Chặn một người dùng khác
    void blockUser(String userId, String blockedId);
    
    // Bỏ chặn người dùng
    void unblockUser(String userId, String blockedId);
    
    // Xem danh sách những người mình đang chặn
    List<UserSummaryResponse> getBlockList(String userId);
    
    // Kiểm tra xem user A có đang chặn user B hay không (hoặc ngược lại)
    boolean isBlocked(String userId, String targetUserId);
}