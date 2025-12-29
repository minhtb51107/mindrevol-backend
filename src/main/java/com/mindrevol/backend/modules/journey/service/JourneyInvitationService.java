package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JourneyInvitationService {
    
    // [FIX] Đổi UUID -> Long
    void inviteFriendToJourney(User inviter, Long journeyId, Long friendId);

    // Chấp nhận lời mời
    void acceptInvitation(User currentUser, Long invitationId);

    // Từ chối lời mời
    void rejectInvitation(User currentUser, Long invitationId);

    // Lấy danh sách lời mời đang chờ tôi duyệt
    Page<JourneyInvitationResponse> getMyPendingInvitations(User currentUser, Pageable pageable);
}