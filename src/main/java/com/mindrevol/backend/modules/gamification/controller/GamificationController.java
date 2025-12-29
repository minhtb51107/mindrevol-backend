package com.mindrevol.backend.modules.gamification.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.gamification.enabled", havingValue = "true")
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserService userService;

    // [ĐÃ XÓA] buy-freeze-streak, repair-streak

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getMyBadges() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getAllBadgesWithStatus(currentUser)));
    }

    @GetMapping("/points/history")
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getPointHistory() {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getPointHistory(currentUser)));
    }
}