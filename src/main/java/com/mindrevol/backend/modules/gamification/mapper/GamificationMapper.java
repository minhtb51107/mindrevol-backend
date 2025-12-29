package com.mindrevol.backend.modules.gamification.mapper;

import com.mindrevol.backend.modules.gamification.dto.response.BadgeResponse;
import com.mindrevol.backend.modules.gamification.dto.response.PointHistoryResponse;
import com.mindrevol.backend.modules.gamification.entity.Badge;
import com.mindrevol.backend.modules.gamification.entity.PointHistory;
import com.mindrevol.backend.modules.gamification.entity.UserBadge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface GamificationMapper {

    // 1. Map từ UserBadge -> BadgeResponse
    @Mapping(target = "id", source = "badge.id")
    @Mapping(target = "code", source = "badge.code")
    @Mapping(target = "name", source = "badge.name")
    @Mapping(target = "description", source = "badge.description")
    @Mapping(target = "iconUrl", source = "badge.iconUrl")
    @Mapping(target = "conditionType", source = "badge.conditionType")
    @Mapping(target = "requiredValue", source = "badge.conditionValue")
    
    // [FIX] Sửa 'earnedAt' thành 'createdAt' (do kế thừa BaseEntity)
    @Mapping(target = "obtainedAt", source = "createdAt") 
    @Mapping(target = "isOwned", constant = "true") 
    BadgeResponse toResponse(UserBadge userBadge);

    // 2. Map từ Badge -> BadgeResponse
    @Mapping(target = "code", source = "code")
    @Mapping(target = "requiredValue", source = "conditionValue")
    @Mapping(target = "isOwned", ignore = true)
    @Mapping(target = "obtainedAt", ignore = true)
    BadgeResponse toBadgeResponse(Badge badge);

    @Mapping(target = "createdAt", source = "createdAt")
    PointHistoryResponse toPointHistoryResponse(PointHistory history);

    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}