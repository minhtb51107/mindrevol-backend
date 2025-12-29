package com.mindrevol.backend.modules.journey.mapper;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitation;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse; // Import này có thể khác tùy module user của bạn
import org.springframework.stereotype.Component;

@Component
public class JourneyMapper {

    public JourneyResponse toResponse(Journey journey) {
        if (journey == null) return null;

        return JourneyResponse.builder()
                .id(journey.getId())
                .name(journey.getName())
                .description(journey.getDescription())
                .startDate(journey.getStartDate())
                .endDate(journey.getEndDate())
                .visibility(journey.getVisibility())
                .status(journey.getStatus())
                .inviteCode(journey.getInviteCode())
                .build();
    }

    public JourneyInvitationResponse toInvitationResponse(JourneyInvitation invitation) {
        if (invitation == null) return null;

        return JourneyInvitationResponse.builder()
                .id(invitation.getId())
                .journeyId(invitation.getJourney().getId()) // Trả về Long ID
                .journeyName(invitation.getJourney().getName())
                .inviterName(invitation.getInviter().getFullname()) // Map thẳng tên String
                .inviterAvatar(invitation.getInviter().getAvatarUrl()) // Map thẳng avatar String
                .status(invitation.getStatus())
                .sentAt(invitation.getCreatedAt())
                .build();
    }
}