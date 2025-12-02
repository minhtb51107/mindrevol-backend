package com.mindrevol.backend.modules.user.mapper;

import com.mindrevol.backend.modules.user.dto.response.FriendshipResponse;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.Friendship;
import com.mindrevol.backend.modules.user.entity.User;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface FriendshipMapper {

    @Mapping(target = "friend", source = "friendship", qualifiedByName = "mapFriend")
    @Mapping(target = "isRequester", source = "friendship", qualifiedByName = "checkRequester")
    @Mapping(target = "createdAt", source = "createdAt")
    FriendshipResponse toResponse(Friendship friendship, @Context Long currentUserId);

    @Named("mapFriend")
    default UserSummaryResponse mapFriend(Friendship f, @Context Long currentUserId) {
        User friend = f.getRequester().getId().equals(currentUserId) ? f.getAddressee() : f.getRequester();
        return UserSummaryResponse.builder()
                .id(friend.getId())
                .fullname(friend.getFullname())
                .handle(friend.getHandle())
                .avatarUrl(friend.getAvatarUrl())
                .build();
    }

    @Named("checkRequester")
    default boolean checkRequester(Friendship f, @Context Long currentUserId) {
        return f.getRequester().getId().equals(currentUserId);
    }

    // --- THÊM HÀM NÀY ĐỂ FIX LỖI ---
    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}