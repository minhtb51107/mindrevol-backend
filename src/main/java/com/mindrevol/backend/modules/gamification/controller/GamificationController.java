package com.mindrevol.backend.modules.gamification.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "Điểm thưởng, Huy hiệu")
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/badges/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách huy hiệu của tôi")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getMyBadges() {
        String userId = SecurityUtils.getCurrentUserId(); // [UUID]
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getMyBadges(userId)));
    }

    @GetMapping("/points/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy lịch sử điểm thưởng")
    public ResponseEntity<ApiResponse<Page<PointHistoryResponse>>> getPointHistory(Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserId(); // [UUID]
        return ResponseEntity.ok(ApiResponse.success(gamificationService.getMyPointHistory(userId, pageable)));
    }
}