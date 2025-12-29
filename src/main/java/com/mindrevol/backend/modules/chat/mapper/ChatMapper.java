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
    
    // [FIX] Map explicit từ property 'deleted' (do getter isDeleted) sang field 'isDeleted'
    @Mapping(target = "isDeleted", source = "deleted") 
    MessageResponse toResponse(Message message);
}