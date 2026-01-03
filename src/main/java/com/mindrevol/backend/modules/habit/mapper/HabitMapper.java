package com.mindrevol.backend.modules.habit.mapper;

import com.mindrevol.backend.modules.habit.dto.response.HabitResponse;
import com.mindrevol.backend.modules.habit.entity.Habit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HabitMapper {

    @Mapping(target = "userId", source = "user.id") // User ID là String -> OK
    @Mapping(target = "journeyId", source = "journeyId") // String -> OK
    @Mapping(target = "isCompletedToday", ignore = true)
    HabitResponse toResponse(Habit habit);
}