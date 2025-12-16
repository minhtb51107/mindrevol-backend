package com.mindrevol.backend.modules.gamification.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserService userService;

    @PostMapping("/buy-freeze-streak")
    public ResponseEntity<ApiResponse<String>> buyFreezeStreak() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        gamificationService.buyFreezeStreakItem(currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi vé Nghỉ Phép thành công!"));
    }

    @PostMapping("/repair-streak")
    public ResponseEntity<ApiResponse<Void>> repairStreak(@RequestParam UUID journeyId) {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        gamificationService.repairStreak(journeyId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Khôi phục chuỗi thành công! 🔥"));
    }

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getMyBadges() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        // CẬP NHẬT: Gọi hàm mới lấy Full Badge kèm trạng thái
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getAllBadgesWithStatus(currentUser)));
    }

    @GetMapping("/points/history")
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getPointHistory() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getPointHistory(currentUser)));
    }
}