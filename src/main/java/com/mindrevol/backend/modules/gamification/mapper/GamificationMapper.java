package com.mindrevol.backend.modules.gamification.mapper;

import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface GamificationMapper {

    @Mapping(target = "id", source = "badge.id")
    @Mapping(target = "code", source = "badge.code")
    @Mapping(target = "name", source = "badge.name")
    @Mapping(target = "description", source = "badge.description")
    @Mapping(target = "iconUrl", source = "badge.iconUrl")
    @Mapping(target = "earnedAt", source = "earnedAt")
    BadgeResponse toResponse(UserBadge userBadge);

    @Mapping(target = "createdAt", source = "createdAt")
    PointHistoryResponse toPointHistoryResponse(PointHistory history);

    // --- THÊM HÀM NÀY ĐỂ FIX LỖI ---
    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}