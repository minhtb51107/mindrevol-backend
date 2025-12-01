package com.mindrevol.backend.modules.checkin.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.request.ReactionRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.service.CheckinService;
import com.mindrevol.backend.modules.checkin.service.ReactionService;
import com.mindrevol.backend.modules.user.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final ReactionService reactionService;

    // API Post bài (Upload ảnh)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckinResponse>> createCheckin(
            @ModelAttribute @Valid CheckinRequest request, // @ModelAttribute dùng cho form-data
            @AuthenticationPrincipal User currentUser) {
        
        CheckinResponse response = checkinService.createCheckin(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Check-in thành công"));
    }

    // API Lấy Feed (Grid) của 1 Journey
    @GetMapping("/journey/{journeyId}")
    public ResponseEntity<ApiResponse<Page<CheckinResponse>>> getJourneyFeed(
            @PathVariable UUID journeyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        
        Page<CheckinResponse> response = checkinService.getJourneyFeed(journeyId, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/reaction")
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @RequestBody @Valid ReactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        reactionService.toggleReaction(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Success"));
    }
}