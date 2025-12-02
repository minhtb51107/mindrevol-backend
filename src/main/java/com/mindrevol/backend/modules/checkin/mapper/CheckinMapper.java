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
    CheckinResponse toResponse(Checkin checkin);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    CommentResponse toCommentResponse(CheckinComment comment);

    // --- THÊM HÀM NÀY ĐỂ FIX LỖI ---
    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}