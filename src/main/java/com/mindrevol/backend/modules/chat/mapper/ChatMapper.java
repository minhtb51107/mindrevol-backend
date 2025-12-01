package com.mindrevol.backend.modules.chat.mapper;

import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.fullname")
    @Mapping(target = "senderAvatar", source = "sender.avatarUrl")
    
    @Mapping(target = "replyToCheckinId", source = "replyToCheckin.id")
    @Mapping(target = "replyToCheckinThumbnail", source = "replyToCheckin.thumbnailUrl")

    @Mapping(target = "isRead", source = "read") 
    
    MessageResponse toResponse(Message message);
}