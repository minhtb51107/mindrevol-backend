package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import java.util.List;
import java.util.UUID;

public interface ReactionService {
    void toggleReaction(UUID checkinId, Long userId, String emoji, String mediaUrl);
    List<CheckinReactionDetailResponse> getReactions(UUID checkinId); // Cho Modal (Full)
    List<CheckinReactionDetailResponse> getPreviewReactions(UUID checkinId); // Cho FacePile (Top 3)
}