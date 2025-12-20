package com.mindrevol.backend.modules.checkin.mapper;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import com.mindrevol.backend.modules.checkin.entity.CheckinReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReactionMapper {

    // Map Reaction (Thả tim -> DTO)
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "type", constant = "REACTION") 
    @Mapping(target = "content", ignore = true) // <--- THÊM DÒNG NÀY (Reaction không có content)
    CheckinReactionDetailResponse toDetailResponse(CheckinReaction entity);

    // Map Comment (Bình luận -> DTO)
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "type", constant = "COMMENT") 
    @Mapping(target = "content", source = "content")
    @Mapping(target = "emoji", ignore = true) // Comment không có emoji
    @Mapping(target = "mediaUrl", ignore = true)
    CheckinReactionDetailResponse toDetailResponseFromComment(CheckinComment entity);
}