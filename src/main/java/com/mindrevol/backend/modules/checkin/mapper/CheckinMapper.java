package com.mindrevol.backend.modules.checkin.mapper;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CheckinMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "userFullName", source = "user.fullname")
    
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskTitle", source = "task.title")
    @Mapping(target = "taskDayNo", source = "task.dayNo")
    
    CheckinResponse toResponse(Checkin checkin);
}