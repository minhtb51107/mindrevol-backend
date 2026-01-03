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
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final UserService userService;
    private final ReactionService reactionService;

    // --- MAIN FEED ---

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<CheckinResponse>> createCheckin(@ModelAttribute @Valid CheckinRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        CheckinResponse response = checkinService.createCheckin(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unified")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getUnifiedFeed(
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(checkinService.getUnifiedFeed(currentUser, cursor, limit)));
    }

    // [UUID] journeyId là String
    @GetMapping("/journey/{journeyId}")
    public ResponseEntity<ApiResponse<List<CheckinResponse>>> getJourneyFeed(
            @PathVariable String journeyId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(checkinService.getJourneyFeedByCursor(journeyId, currentUser, cursor, limit)));
    }

    // --- INTERACTIONS ---

    @PostMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> postComment(
            @PathVariable String checkinId,
            @RequestBody String content) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(checkinService.postComment(checkinId, content, currentUser)));
    }

    @GetMapping("/{checkinId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable String checkinId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(checkinService.getComments(checkinId, pageable)));
    }

    @PostMapping("/{checkinId}/reactions")
    public ResponseEntity<ApiResponse<Void>> toggleReaction(
            @PathVariable String checkinId,
            @RequestBody @Valid ReactionRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        reactionService.toggleReaction(checkinId, userId, request.getEmoji(), request.getMediaUrl());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{checkinId}/reactions")
    public ResponseEntity<ApiResponse<List<CheckinReactionDetailResponse>>> getReactions(@PathVariable String checkinId) {
        return ResponseEntity.ok(ApiResponse.success(reactionService.getReactions(checkinId)));
    }

    // --- ACTIONS ---

    @PutMapping("/{checkinId}")
    public ResponseEntity<ApiResponse<CheckinResponse>> updateCheckin(
            @PathVariable String checkinId,
            @RequestBody String caption) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(checkinService.updateCheckin(checkinId, caption, currentUser)));
    }

    @DeleteMapping("/{checkinId}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckin(@PathVariable String checkinId) {
        String userId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserById(userId);
        checkinService.deleteCheckin(checkinId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa bài viết thành công"));
    }
}