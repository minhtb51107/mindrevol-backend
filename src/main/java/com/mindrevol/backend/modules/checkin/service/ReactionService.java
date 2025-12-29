package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import java.util.List;

public interface ReactionService {
    // [FIX] UUID -> Long
    void toggleReaction(Long checkinId, Long userId, String emoji, String mediaUrl);
    List<CheckinReactionDetailResponse> getReactions(Long checkinId);
    List<CheckinReactionDetailResponse> getPreviewReactions(Long checkinId);
}