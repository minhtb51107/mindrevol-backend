package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JourneyInvitationService {
    
    // Mời bạn vào nhóm
    void inviteFriendToJourney(User inviter, UUID journeyId, Long friendId);

    // Chấp nhận lời mời
    void acceptInvitation(User currentUser, Long invitationId);

    // Từ chối lời mời
    void rejectInvitation(User currentUser, Long invitationId);

    // Lấy danh sách lời mời đang chờ tôi duyệt
    Page<JourneyInvitationResponse> getMyPendingInvitations(User currentUser, Pageable pageable);
}