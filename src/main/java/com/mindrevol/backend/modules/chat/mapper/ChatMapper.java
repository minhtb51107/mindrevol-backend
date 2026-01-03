package com.mindrevol.backend.modules.chat.mapper;

import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderAvatar", source = "sender.avatarUrl")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "isDeleted", source = "deleted") 
    MessageResponse toResponse(Message message);
}