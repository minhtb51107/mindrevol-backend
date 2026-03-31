package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import java.util.List;

/**
 * Service quản lý việc người dùng thả cảm xúc (react) vào các bài check-in.
 */
public interface ReactionService {
    // Bật/tắt reaction (Nếu đã thả emoji này rồi thì xóa, chưa thì thêm mới hoặc cập nhật)
    void toggleReaction(String checkinId, String userId, String emoji, String mediaUrl);
    
    // Lấy danh sách chi tiết tất cả người dùng đã thả cảm xúc vào bài check-in
    List<CheckinReactionDetailResponse> getReactions(String checkinId);
    
    // Lấy danh sách xem trước reaction (thường là 3-5 avatar/người đầu tiên) để hiển thị nhanh
    List<CheckinReactionDetailResponse> getPreviewReactions(String checkinId);
}