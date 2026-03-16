package com.mindrevol.backend.modules.mood.mapper;

import com.mindrevol.backend.modules.mood.dto.response.MoodReactionResponse;
import com.mindrevol.backend.modules.mood.dto.response.MoodResponse;
import com.mindrevol.backend.modules.mood.entity.Mood;
import com.mindrevol.backend.modules.mood.entity.MoodReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MoodMapper {

    @Mapping(source = "box.id", target = "boxId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullname", target = "fullname")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    MoodResponse toResponse(Mood mood);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.fullname", target = "fullname")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    MoodReactionResponse toReactionResponse(MoodReaction reaction);
}