package com.mindrevol.backend.modules.habit.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.habit.dto.request.CreateHabitRequest;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.user.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @Valid @RequestBody CreateHabitRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(habitService.createHabit(request, user), "Tạo thói quen thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HabitResponse>>> getMyHabits(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(habitService.getMyHabits(user)));
    }

    // Đánh dấu hoàn thành (Check-in nhanh không cần ảnh)
    @PostMapping("/{habitId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeHabit(
            @PathVariable UUID habitId,
            @AuthenticationPrincipal User user) {
        habitService.markHabitCompleted(habitId, null, user);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn thành thói quen"));
    }
    
    // Đánh dấu thất bại (Fail)
    @PostMapping("/{habitId}/fail")
    public ResponseEntity<ApiResponse<Void>> failHabit(
            @PathVariable UUID habitId,
            @AuthenticationPrincipal User user) {
        habitService.markHabitFailed(habitId, user);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận trạng thái"));
    }
}