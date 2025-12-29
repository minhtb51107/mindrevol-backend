package com.mindrevol.backend.modules.checkin.mapper;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface CheckinMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "journeyId", source = "journey.id") // [FIX] Thêm dòng này để hết Warning
    
    @Mapping(target = "commentCount", expression = "java((long) (checkin.getComments() != null ? checkin.getComments().size() : 0))")
    @Mapping(target = "reactionCount", expression = "java((long) (checkin.getReactions() != null ? checkin.getReactions().size() : 0))")
    @Mapping(target = "latestReactions", ignore = true)
    CheckinResponse toResponse(Checkin checkin);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    CommentResponse toCommentResponse(CheckinComment comment);
}