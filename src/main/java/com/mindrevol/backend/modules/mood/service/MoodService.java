package com.mindrevol.backend.modules.mood.service;

import com.mindrevol.backend.modules.mood.dto.request.MoodRequest;
import com.mindrevol.backend.modules.mood.dto.response.MoodResponse;

import java.util.List;

public interface MoodService {
    MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request);
    List<MoodResponse> getActiveMoodsInBox(String boxId);
    void deleteMyMood(String boxId, String userId);
    void reactToMood(String moodId, String userId, String emoji);
    void removeReaction(String moodId, String userId);
}