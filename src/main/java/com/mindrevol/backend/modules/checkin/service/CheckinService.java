package com.mindrevol.backend.modules.checkin.service;

import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinService {

    CheckinResponse createCheckin(CheckinRequest request, User currentUser);

    // [FIX] UUID -> Long
    Page<CheckinResponse> getJourneyFeed(Long journeyId, Pageable pageable, User currentUser);

    CommentResponse postComment(Long checkinId, String content, User currentUser);
    
    Page<CommentResponse> getComments(Long checkinId, Pageable pageable);

    List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit);

    // [FIX] UUID -> Long
    List<CheckinResponse> getJourneyFeedByCursor(Long journeyId, User currentUser, LocalDateTime cursor, int limit);
    
    CheckinResponse updateCheckin(Long checkinId, String caption, User currentUser);

    void deleteCheckin(Long checkinId, User currentUser);
}