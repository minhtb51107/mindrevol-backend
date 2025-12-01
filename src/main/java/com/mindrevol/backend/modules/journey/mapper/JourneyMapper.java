package com.mindrevol.backend.modules.journey.mapper;

import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.entity.Journey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JourneyMapper {

    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "creatorName", source = "creator.fullname")
    @Mapping(target = "creatorAvatar", source = "creator.avatarUrl")
    @Mapping(target = "participantCount", expression = "java(journey.getParticipants() != null ? journey.getParticipants().size() : 0)")
    
    @Mapping(target = "isJoined", ignore = true)
    @Mapping(target = "inviteUrl", ignore = true)
    @Mapping(target = "qrCodeData", ignore = true)
    
    JourneyResponse toResponse(Journey journey);
}