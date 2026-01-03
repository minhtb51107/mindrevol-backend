package com.mindrevol.backend.modules.habit.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.habit.dto.request.CreateHabitRequest;
import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.habit.service.HabitService;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // [UUID] @PathVariable String habitId
    @PostMapping("/{habitId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeHabit(
            @PathVariable String habitId,
            @AuthenticationPrincipal User user) {
        habitService.markHabitCompleted(habitId, null, user);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn thành thói quen"));
    }
    
    // [UUID] @PathVariable String habitId
    @PostMapping("/{habitId}/fail")
    public ResponseEntity<ApiResponse<Void>> failHabit(
            @PathVariable String habitId,
            @AuthenticationPrincipal User user) {
        habitService.markHabitFailed(habitId, user);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận trạng thái"));
    }
}