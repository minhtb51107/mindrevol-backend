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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindrevol.backend.common.dto.ApiResponse;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.request.ReactionRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
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
    
    @PostMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReactionRequest request) { // Request đã sửa ở Bước 3
        
        Long userId = SecurityUtils.getCurrentUserId();
        // Lưu ý: request.getCheckinId() trong DTO có thể dư thừa nếu dùng @PathVariable, 
        // nhưng nếu bạn muốn giữ DTO cũ thì dùng id từ path cho chắc chắn.
        reactionService.toggleReaction(id, userId, request.getEmoji(), request.getMediaUrl());
        
        return ResponseEntity.ok(ApiResponse.success(null, "Tương tác thành công"));
    }

    // THÊM: API Lấy danh sách reactions (Cho Activity Modal)
    @GetMapping("/{id}/reactions")
    public ResponseEntity<ApiResponse<List<CheckinReactionDetailResponse>>> getReactions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reactionService.getReactions(id)));
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
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckinResponse>> updateCheckin(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        
        String caption = body.get("caption");
        // Gọi service update (bạn tự implement update đơn giản chỉ setCaption và save)
        CheckinResponse response = checkinService.updateCheckin(id, caption, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckin(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        
        checkinService.deleteCheckin(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa bài viết"));
    }
}