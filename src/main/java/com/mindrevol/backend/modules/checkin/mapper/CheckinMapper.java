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
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskTitle", source = "task.title")
    @Mapping(target = "taskDayNo", source = "task.dayNo")
    
    // --- [THÊM MỚI] Mapping số lượng tương tác ---
    @Mapping(target = "commentCount", expression = "java((long) (checkin.getComments() != null ? checkin.getComments().size() : 0))")
    @Mapping(target = "reactionCount", expression = "java((long) (checkin.getReactions() != null ? checkin.getReactions().size() : 0))")
    
    // --- [THÊM MỚI] Ignore list reaction chi tiết (Service sẽ tự điền sau) ---
    @Mapping(target = "latestReactions", ignore = true)
    
    CheckinResponse toResponse(Checkin checkin);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    CommentResponse toCommentResponse(CheckinComment comment);

    // --- Giữ nguyên hàm helper của bạn ---
    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}