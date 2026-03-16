package com.mindrevol.backend.modules.mood.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.mood.dto.request.MoodRequest;
import com.mindrevol.backend.modules.mood.dto.response.MoodResponse;
import com.mindrevol.backend.modules.mood.service.MoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MoodController {

    private final MoodService moodService;

    @PostMapping("/boxes/{boxId}/moods")
    public ApiResponse<MoodResponse> createOrUpdateMood(
            @PathVariable String boxId,
            @Valid @RequestBody MoodRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(moodService.createOrUpdateMood(boxId, userId, request));
    }

    @GetMapping("/boxes/{boxId}/moods")
    public ApiResponse<List<MoodResponse>> getActiveMoods(@PathVariable String boxId) {
        return ApiResponse.success(moodService.getActiveMoodsInBox(boxId));
    }

    @DeleteMapping("/boxes/{boxId}/moods")
    public ApiResponse<Void> deleteMyMood(@PathVariable String boxId) {
        String userId = SecurityUtils.getCurrentUserId();
        moodService.deleteMyMood(boxId, userId);
        return ApiResponse.success(null, "Đã xóa trạng thái");
    }

    @PostMapping("/moods/{moodId}/reactions")
    public ApiResponse<Void> reactToMood(
            @PathVariable String moodId,
            @RequestParam String emoji) {
        String userId = SecurityUtils.getCurrentUserId();
        moodService.reactToMood(moodId, userId, emoji);
        return ApiResponse.success(null, "Đã thả cảm xúc");
    }

    @DeleteMapping("/moods/{moodId}/reactions")
    public ApiResponse<Void> removeReaction(@PathVariable String moodId) {
        String userId = SecurityUtils.getCurrentUserId();
        moodService.removeReaction(moodId, userId);
        return ApiResponse.success(null, "Đã gỡ cảm xúc");
    }
}