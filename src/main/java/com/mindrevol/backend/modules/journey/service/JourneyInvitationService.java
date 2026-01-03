package com.mindrevol.backend.modules.journey.service;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JourneyInvitationService {
    
    // [UUID] String journeyId, String friendId
    void inviteFriendToJourney(User inviter, String journeyId, String friendId);

    void acceptInvitation(User currentUser, String invitationId);

    void rejectInvitation(User currentUser, String invitationId);

    Page<JourneyInvitationResponse> getMyPendingInvitations(User currentUser, Pageable pageable);
}