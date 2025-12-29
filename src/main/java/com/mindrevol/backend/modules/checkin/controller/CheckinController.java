package com.mindrevol.backend.modules.checkin.controller;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.request.ReactionRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.checkin.service.CheckinService;
import com.mindrevol.backend.modules.checkin.service.ReactionService;
import com.mindrevol.backend.modules.feed.service.FeedService;
import com.mindrevol.backend.modules.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final ReactionService reactionService;
    private final FeedService feedService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckinResponse>> createCheckin(
            @ModelAttribute @Valid CheckinRequest request, 
            @AuthenticationPrincipal User currentUser) {
        CheckinResponse response = checkinService.createCheckin(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Check-in thành công"));
    }

    // [FIX] UUID -> Long
    @GetMapping("/journey/{journeyId}")
    public ResponseEntity<ApiResponse<Page<CheckinResponse>>> getJourneyFeed(
            @PathVariable("journeyId") Long journeyId, 
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<CheckinResponse> response = checkinService.getJourneyFeed(journeyId, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // [FIX] UUID -> Long
    @PostMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @PathVariable Long id,
            @Valid @RequestBody ReactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        reactionService.toggleReaction(id, userId, request.getEmoji(), request.getMediaUrl());
        return ResponseEntity.ok(ApiResponse.success(null, "Tương tác thành công"));
    }

    // [FIX] UUID -> Long
    @GetMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<List<CheckinReactionDetailResponse>>> getReactions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reactionService.getReactions(id)));
    }
    
    // [FIX] UUID -> Long
    @PostMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> postComment(
            @PathVariable("checkinId") Long checkinId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new com.mindrevol.backend.common.exception.BadRequestException("Nội dung không được để trống");
        }
        CommentResponse response = checkinService.postComment(checkinId, content, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // [FIX] UUID -> Long
    @GetMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable("checkinId") Long checkinId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CommentResponse> response = checkinService.getComments(checkinId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getHomeFeed(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal User currentUser) {
        List<CheckinResponse> feed = feedService.getNewsFeed(currentUser.getId(), page, limit); // Chú ý: tên hàm bên Service là getNewsFeed
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    // [FIX] UUID -> Long
    @GetMapping("/journey/{journeyId}/cursor")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getJourneyFeedCursor(
            @PathVariable("journeyId") Long journeyId,
            @RequestParam(value = "cursor", required = false) LocalDateTime cursor,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal User currentUser) {
        List<CheckinResponse> feed = checkinService.getJourneyFeedByCursor(journeyId, currentUser, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }
    
    // [FIX] UUID -> Long
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckinResponse>> updateCheckin(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String caption = body.get("caption");
        CheckinResponse response = checkinService.updateCheckin(id, caption, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thành công"));
    }

    // [FIX] UUID -> Long
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckin(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        checkinService.deleteCheckin(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa bài viết"));
    }
}