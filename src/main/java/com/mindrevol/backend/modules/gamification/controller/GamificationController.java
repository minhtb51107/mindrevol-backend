package com.mindrevol.backend.modules.gamification.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService; // Inject để lấy User full
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity; // Import
import org.springframework.web.bind.annotation.GetMapping; // Import
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserService userService;

    @PostMapping("/buy-freeze-streak")
    public ResponseEntity<ApiResponse<String>> buyFreezeStreak() {
        // Lấy User Entity đầy đủ từ DB để đảm bảo data mới nhất (điểm số)
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        
        gamificationService.buyFreezeStreakItem(currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi vé Nghỉ Phép thành công!"));
    }

    // --- CÁC API MỚI ---

    // 1. Lấy danh sách Huy hiệu
    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getMyBadges() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getUserBadges(currentUser)));
    }

    // 2. Lấy lịch sử điểm (Wallet History)
    @GetMapping("/points/history")
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getPointHistory() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getPointHistory(currentUser)));
    }
}