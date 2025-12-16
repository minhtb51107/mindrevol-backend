package com.mindrevol.backend.modules.checkin.controller;

import java.util.UUID;

import com.mindrevol.backend.modules.feed.service.FeedService;
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

import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse; 
import java.util.Map; 
import java.time.LocalDateTime; 
import java.util.List; 
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final ReactionService reactionService;
    private final FeedService feedService;

    // API Post bài (Upload ảnh)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckinResponse>> createCheckin(
            @ModelAttribute @Valid CheckinRequest request, 
            @AuthenticationPrincipal User currentUser) {
        
        CheckinResponse response = checkinService.createCheckin(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Check-in thành công"));
    }

    // API Lấy Feed (Grid) của 1 Journey
    @GetMapping("/journey/{journeyId}")
    public ResponseEntity<ApiResponse<Page<CheckinResponse>>> getJourneyFeed(
            @PathVariable("journeyId") UUID journeyId, // <--- Đã thêm ("journeyId")
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
    
    @PostMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> postComment(
            @PathVariable("checkinId") UUID checkinId, // <--- Đã thêm ("checkinId")
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new com.mindrevol.backend.common.exception.BadRequestException("Nội dung không được để trống");
        }
        
        CommentResponse response = checkinService.postComment(checkinId, content, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable("checkinId") UUID checkinId, // <--- Đã thêm ("checkinId")
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<CommentResponse> response = checkinService.getComments(checkinId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // --- 1. Lấy Feed Tổng Hợp (Home Feed) ---
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getHomeFeed(
            @RequestParam(value = "page", defaultValue = "0") int page,   // <--- Đã thêm value = "page"
            @RequestParam(value = "limit", defaultValue = "10") int limit, // <--- Đã thêm value = "limit"
            @AuthenticationPrincipal User currentUser) {
        
        List<CheckinResponse> feed = feedService.getFeed(currentUser.getId(), page, limit);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    // 2. Lấy Feed Journey theo Cursor
    @GetMapping("/journey/{journeyId}/cursor")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getJourneyFeedCursor(
            @PathVariable("journeyId") UUID journeyId, // <--- Đã thêm ("journeyId")
            @RequestParam(value = "cursor", required = false) LocalDateTime cursor, // <--- Đã thêm value
            @RequestParam(value = "limit", defaultValue = "10") int limit,          // <--- Đã thêm value
            @AuthenticationPrincipal User currentUser) {
        
        List<CheckinResponse> feed = checkinService.getJourneyFeedByCursor(journeyId, currentUser, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }
}