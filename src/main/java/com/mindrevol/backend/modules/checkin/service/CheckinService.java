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

    // [UUID] Long -> String
    Page<CheckinResponse> getJourneyFeed(String journeyId, Pageable pageable, User currentUser);

    CommentResponse postComment(String checkinId, String content, User currentUser);
    
    Page<CommentResponse> getComments(String checkinId, Pageable pageable);

    List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit);

    List<CheckinResponse> getJourneyFeedByCursor(String journeyId, User currentUser, LocalDateTime cursor, int limit);
    
    CheckinResponse updateCheckin(String checkinId, String caption, User currentUser);

    void deleteCheckin(String checkinId, User currentUser);
}