package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service chuyên xử lý việc gửi và nhận lời mời tham gia Hành trình.
 */
public interface JourneyInvitationService {
    
    // Gửi lời mời tới một người bạn
    void inviteFriendToJourney(User inviter, String journeyId, String friendId);

    // Chấp nhận lời mời tham gia
    void acceptInvitation(User currentUser, String invitationId);

    // Từ chối lời mời
    void rejectInvitation(User currentUser, String invitationId);

    // Xem danh sách các lời mời đang chờ xử lý của bản thân
    Page<JourneyInvitationResponse> getMyPendingInvitations(User currentUser, Pageable pageable);
}