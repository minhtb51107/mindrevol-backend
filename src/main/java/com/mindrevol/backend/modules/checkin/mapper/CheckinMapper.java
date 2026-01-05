package com.mindrevol.backend.modules.checkin.mapper;

import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinComment;
import com.mindrevol.backend.modules.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class}) // [QUAN TRỌNG] Cần uses UserMapper để map object User
public interface CheckinMapper {

    // --- 1. Map cho Checkin (Cấu trúc MỚI - Object lồng nhau) ---
    @Mapping(target = "user", source = "user") // MapStruct sẽ dùng UserMapper để chuyển User entity -> UserSummaryResponse
    @Mapping(target = "journeyId", source = "journey.id")
    @Mapping(target = "journeyName", source = "journey.name")
    
    // Stats calculation
    @Mapping(target = "commentCount", expression = "java((int) (checkin.getComments() != null ? checkin.getComments().size() : 0))")
    @Mapping(target = "reactionCount", expression = "java((int) (checkin.getReactions() != null ? checkin.getReactions().size() : 0))")
    
    // Ignore field này vì sẽ được enrich sau trong Service
    @Mapping(target = "latestReactions", ignore = true)
    
    // Các trường mediaType, videoUrl, imageFileId sẽ tự động map vì trùng tên
    CheckinResponse toResponse(Checkin checkin);


    // --- 2. Map cho Comment (Cấu trúc CŨ - Trường phẳng) ---
    // Vì bạn muốn giữ CommentResponse dạng phẳng (userId, avatarUrl rời) nên ta map thủ công từng cái
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullname")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    CommentResponse toCommentResponse(CheckinComment comment);
}