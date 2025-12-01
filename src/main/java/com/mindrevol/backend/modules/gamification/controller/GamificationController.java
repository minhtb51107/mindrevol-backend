package com.mindrevol.backend.modules.gamification.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import com.mindrevol.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @PostMapping("/buy-freeze-streak")
    public ApiResponse<String> buyFreezeStreak() {
        User currentUser = SecurityUtils.getCurrentUser();
        
        gamificationService.buyFreezeStreakItem(currentUser);
        
        return ApiResponse.success(null, "Đổi vé Nghỉ Phép thành công!");
    }
}