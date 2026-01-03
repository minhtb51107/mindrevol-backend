package com.mindrevol.backend.modules.gamification.mapper;

import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GamificationMapper {

    @Mapping(target = "earnedAt", source = "earnedAt")
    @Mapping(target = "id", source = "badge.id")
    @Mapping(target = "name", source = "badge.name")
    @Mapping(target = "description", source = "badge.description")
    @Mapping(target = "iconUrl", source = "badge.iconUrl")
    BadgeResponse toUserBadgeResponse(UserBadge userBadge);

    @Mapping(target = "earnedAt", ignore = true)
    BadgeResponse toBadgeResponse(Badge badge);

    PointHistoryResponse toPointHistoryResponse(PointHistory history);
}