package com.mindrevol.backend.modules.mood.service;

import com.mindrevol.backend.modules.mood.dto.request.MoodRequest;
import com.mindrevol.backend.modules.mood.dto.response.MoodResponse;

import java.util.List;

/**
 * Service quản lý việc chia sẻ trạng thái tâm trạng (Mood) trong một Box.
 */
public interface MoodService {
    // Đăng hoặc cập nhật tâm trạng hiện tại trong một Box
    MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request);
    
    // Lấy danh sách tâm trạng đang hoạt động (của mọi người) trong Box
    List<MoodResponse> getActiveMoodsInBox(String boxId);
    
    // Xóa tâm trạng hiện tại của bản thân
    void deleteMyMood(String boxId, String userId);
    
    // Thả emoji/cảm xúc vào tâm trạng của người khác
    void reactToMood(String moodId, String userId, String emoji);
    
    // Xóa emoji/cảm xúc đã thả
    void removeReaction(String moodId, String userId);
}