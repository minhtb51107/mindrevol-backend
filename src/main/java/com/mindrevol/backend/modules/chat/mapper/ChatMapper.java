package com.mindrevol.backend.modules.chat.mapper;

import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    // Sử dụng hàm map custom bên dưới để chuyển đổi thời gian
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "mapOffsetDateTime")
    MessageResponse toResponse(Message message);

    // Hàm chuyển đổi từ OffsetDateTime (Entity) -> LocalDateTime (DTO)
    @Named("mapOffsetDateTime")
    default LocalDateTime mapOffsetDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.toLocalDateTime();
    }
}