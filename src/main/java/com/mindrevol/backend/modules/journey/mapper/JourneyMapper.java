package com.mindrevol.backend.modules.journey.mapper;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyParticipantResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitation;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JourneyMapper {

    private final UserMapper userMapper;

    public JourneyResponse toResponse(Journey journey) {
        if (journey == null) return null;

        String creatorId = (journey.getCreator() != null) ? String.valueOf(journey.getCreator().getId()) : null;

        return JourneyResponse.builder()
                .id(journey.getId())
                .name(journey.getName())
                .description(journey.getDescription())
                .startDate(journey.getStartDate())
                .endDate(journey.getEndDate())
                .visibility(journey.getVisibility())
                .status(journey.getStatus())
                .inviteCode(journey.getInviteCode())
                .creatorId(creatorId)
                .build();
    }

    public JourneyInvitationResponse toInvitationResponse(JourneyInvitation invitation) {
        if (invitation == null) return null;

        return JourneyInvitationResponse.builder()
                .id(invitation.getId())
                .journeyId(invitation.getJourney().getId())
                .journeyName(invitation.getJourney().getName())
                .inviterName(invitation.getInviter().getFullname())
                .inviterAvatar(invitation.getInviter().getAvatarUrl())
                .status(invitation.getStatus())
                
                // [MỚI] Map trạng thái hành trình
                .journeyStatus(invitation.getJourney().getStatus()) 
                
                .sentAt(invitation.getCreatedAt())
                .build();
    }

    public JourneyParticipantResponse toParticipantResponse(JourneyParticipant participant) {
        if (participant == null) return null;

        return JourneyParticipantResponse.builder()
                .id(participant.getId())
                .user(userMapper.toSummaryResponse(participant.getUser()))
                .role(participant.getRole().name())
                .joinedAt(participant.getJoinedAt())
                .currentStreak(participant.getCurrentStreak())
                .totalCheckins(participant.getTotalCheckins())
                .lastCheckinAt(participant.getLastCheckinAt())
                .totalActiveDays(participant.getTotalActiveDays())
                
                // Vẫn giữ lại tính % đơn giản để FE hiển thị nếu muốn
                .presenceRate(JourneyParticipantResponse.calculatePresenceRate(
                        participant.getTotalActiveDays(), participant.getJoinedAt()))
                
                // Trả về mặc định hoặc logic đơn giản
                .activityPersona("NORMAL") 
                .build();
    }
}