package com.mindrevol.backend.modules.checkin.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.service.SavedCheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/saved-checkins")
@RequiredArgsConstructor
public class SavedCheckinController {

    private final SavedCheckinService savedCheckinService;

    @PostMapping("/toggle/{checkinId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleSave(@PathVariable String checkinId) {
        String userId = SecurityUtils.getCurrentUserId();
        boolean isSaved = savedCheckinService.toggleSaveCheckin(userId, checkinId);
        
        String message = isSaved ? "Đã lưu bài viết vào bộ sưu tập" : "Đã bỏ lưu bài viết";
        return ResponseEntity.ok(ApiResponse.success(isSaved, message));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<CheckinResponse>>> getMySavedCheckins(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String userId = SecurityUtils.getCurrentUserId();
        Page<CheckinResponse> savedCheckins = savedCheckinService.getMySavedCheckins(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(savedCheckins, "Lấy danh sách bài đã lưu thành công"));
    }
}