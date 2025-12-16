package com.mindrevol.backend.modules.journey.mapper;

import com.mindrevol.backend.modules.journey.dto.response.JourneyInvitationResponse;
import com.mindrevol.backend.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.backend.modules.journey.dto.response.RoadmapStatusResponse;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyInvitation;
import com.mindrevol.backend.modules.journey.entity.JourneyTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface JourneyMapper {

	@Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "creatorName", source = "creator.fullname")
    @Mapping(target = "creatorAvatar", source = "creator.avatarUrl")
    @Mapping(target = "participantCount", expression = "java(journey.getParticipants() != null ? journey.getParticipants().size() : 0)")
    
    // Map các trường setting (Tên field giống nhau nên MapStruct tự hiểu, nhưng khai báo cho chắc)
    @Mapping(target = "hasStreak", source = "hasStreak")
    @Mapping(target = "requiresFreezeTicket", source = "requiresFreezeTicket")
    @Mapping(target = "isHardcore", source = "hardcore") // Lưu ý: Getter của boolean isHardcore thường là isHardcore()
    @Mapping(target = "requireApproval", source = "requireApproval")
    @Mapping(target = "interactionType", source = "interactionType")
    @Mapping(target = "visibility", source = "visibility")
    
	@Mapping(target = "role", ignore = true)
    @Mapping(target = "isJoined", ignore = true)   // <--- THÊM DÒNG NÀY
    @Mapping(target = "inviteUrl", ignore = true)  // <--- THÊM DÒNG NÀY
    
    JourneyResponse toResponse(Journey journey);

    @Mapping(target = "journeyId", source = "journey.id")
    @Mapping(target = "journeyName", source = "journey.name")
    @Mapping(target = "inviterName", source = "inviter.fullname")
    @Mapping(target = "inviterAvatar", source = "inviter.avatarUrl")
    @Mapping(target = "sentAt", source = "createdAt")
    JourneyInvitationResponse toInvitationResponse(JourneyInvitation invitation);

    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "isCompleted", ignore = true)
    RoadmapStatusResponse toRoadmapResponse(JourneyTask task);

    // --- THÊM HÀM NÀY ĐỂ FIX LỖI ---
    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
}