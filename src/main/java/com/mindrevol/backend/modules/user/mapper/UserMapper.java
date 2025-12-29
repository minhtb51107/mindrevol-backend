package com.mindrevol.backend.modules.user.mapper;

import com.mindrevol.backend.modules.user.dto.request.UpdateProfileRequest;
import com.mindrevol.backend.modules.user.dto.response.UserProfileResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.Role;
import com.mindrevol.backend.modules.user.entity.User;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // Map từ createdAt (Local) -> joinedAt (Offset)
    @Mapping(source = "createdAt", target = "joinedAt")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    @Mapping(target = "followerCount", ignore = true) 
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "isFollowedByCurrentUser", ignore = true)
    UserProfileResponse toProfileResponse(User user);

    UserSummaryResponse toSummaryResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UpdateProfileRequest request, @MappingTarget User user);

    @Named("mapRoles")
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // [FIX] Thêm hàm này để MapStruct tự dùng khi thấy LocalDateTime -> OffsetDateTime
    default OffsetDateTime map(LocalDateTime value) {
        return value != null ? value.atOffset(ZoneOffset.UTC) : null;
    }
}