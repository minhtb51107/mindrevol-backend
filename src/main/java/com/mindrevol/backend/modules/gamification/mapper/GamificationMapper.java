package com.mindrevol.backend.modules.gamification.mapper;

import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GamificationMapper {

    // Map từ Entity UserBadge (bảng nối) ra Response
    // Dữ liệu Badge nằm trong object con 'badge' của UserBadge
    @Mapping(target = "id", source = "badge.id")
    @Mapping(target = "code", source = "badge.code")
    @Mapping(target = "name", source = "badge.name")
    @Mapping(target = "description", source = "badge.description")
    @Mapping(target = "iconUrl", source = "badge.iconUrl")
    @Mapping(target = "earnedAt", source = "earnedAt") // Lấy ngày đạt được từ UserBadge
    BadgeResponse toResponse(UserBadge userBadge);
}