package com.mindrevol.backend.modules.journey.mapper;

import com.mindrevol.backend.modules.journey.dto.response.JourneyParticipantResponse;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring", 
    uses = {UserMapper.class}, 
    imports = {JourneyParticipantResponse.class} 
)
public interface JourneyParticipantMapper {

    @Mapping(target = "activityPersona", constant = "NORMAL")
    @Mapping(
        target = "presenceRate", 
        expression = "java(JourneyParticipantResponse.calculatePresenceRate(participant.getTotalActiveDays(), participant.getJoinedAt()))"
    )
    JourneyParticipantResponse toResponse(JourneyParticipant participant);
}