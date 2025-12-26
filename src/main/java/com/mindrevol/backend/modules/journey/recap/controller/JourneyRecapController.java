package com.mindrevol.backend.modules.journey.recap.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.journey.recap.service.JourneyRecapService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
@Tag(name = "Journey Recap", description = "Xem lại kỷ niệm hành trình (Album cá nhân)")
public class JourneyRecapController {

    private final JourneyRecapService journeyRecapService;
    private final UserService userService;

    @GetMapping("/{id}/recap")
    @Operation(summary = "Lấy album ảnh/bài đăng của user trong hành trình (Dạng Instagram Grid)")
    public ResponseEntity<ApiResponse<Page<CheckinResponse>>> getMyJourneyRecap(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User currentUser = userService.getUserById(SecurityUtils.getCurrentUserId());
        
        Page<CheckinResponse> recapFeed = journeyRecapService.getUserRecapFeed(id, currentUser, pageable);

        return ResponseEntity.ok(ApiResponse.success(recapFeed));
    }
}