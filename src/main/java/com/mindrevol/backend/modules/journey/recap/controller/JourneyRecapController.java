package com.mindrevol.backend.modules.journey.recap.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.journey.recap.dto.JourneyRecapResponse;
import com.mindrevol.backend.modules.journey.recap.dto.MemoryGridItem;
import com.mindrevol.backend.modules.journey.recap.service.JourneyRecapService;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
@RequiredArgsConstructor
public class JourneyRecapController {

    private final CheckinRepository checkinRepository;
    private final JourneyParticipantRepository participantRepository;
    private final JourneyRecapService journeyRecapService;
    private final UserService userService;

    // API Lấy Lưới Ký Ức (Memory Grid)
    @GetMapping("/{journeyId}/memory-grid")
    public ResponseEntity<ApiResponse<Page<MemoryGridItem>>> getMemoryGrid(
            @PathVariable UUID journeyId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // 1. Check quyền: Phải là thành viên mới được xem
        if (!participantRepository.existsByJourneyIdAndUserId(journeyId, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem hành trình này");
        }

        // 2. Query DB lấy Checkin, Map sang DTO nhẹ
        Page<MemoryGridItem> grid = checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId, pageable)
                .map(checkin -> MemoryGridItem.builder()
                        .checkinId(checkin.getId())
                        .thumbnailUrl(checkin.getThumbnailUrl() != null ? checkin.getThumbnailUrl() : checkin.getImageUrl())
                        .status(checkin.getStatus())
                        .emotion(checkin.getEmotion())
                        .createdAt(checkin.getCreatedAt())
                        // .reactionCount(...) // Nếu cần chính xác thì count từ bảng reaction
                        .build());

        return ResponseEntity.ok(ApiResponse.success(grid));
    }
    
    @GetMapping("/{journeyId}/recap")
    public ResponseEntity<ApiResponse<JourneyRecapResponse>> getJourneyRecap(@PathVariable UUID journeyId) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(currentUserId);
        
        JourneyRecapResponse response = journeyRecapService.generateRecap(currentUser, journeyId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}