package com.mindrevol.backend.modules.checkin.service;

import java.util.List; // Import List
import java.time.LocalDateTime; // Import LocalDateTime
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.user.entity.User;

public interface CheckinService {

    CheckinResponse createCheckin(CheckinRequest request, User currentUser);

    Page<CheckinResponse> getJourneyFeed(UUID journeyId, Pageable pageable, User currentUser);

    CommentResponse postComment(UUID checkinId, String content, User currentUser);
    
    Page<CommentResponse> getComments(UUID checkinId, Pageable pageable);

    // --- CÁC HÀM FEED MỚI ---
    List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit);

    List<CheckinResponse> getJourneyFeedByCursor(UUID journeyId, User currentUser, LocalDateTime cursor, int limit);
    // ------------------------
}