package com.mindrevol.backend.modules.chat.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Conversation;
import com.mindrevol.backend.modules.chat.entity.Message;
import com.mindrevol.backend.modules.chat.mapper.ChatMapper; // Import Mapper
import com.mindrevol.backend.modules.chat.repository.ConversationRepository;
import com.mindrevol.backend.modules.chat.repository.MessageRepository;
import com.mindrevol.backend.modules.chat.service.ChatService;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMapper chatMapper; // Inject Mapper

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, User sender) {
        // 1. Tìm người nhận
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Người nhận không tồn tại"));

        // 2. Get or Create Conversation (Logic Zalo)
        Conversation conversation = conversationRepository.findByUsers(sender.getId(), receiver.getId())
                .orElseGet(() -> createNewConversation(sender, receiver));

        // 3. Xử lý Reply Context (Nếu có)
        Checkin replyCheckin = null;
        if (request.getReplyToCheckinId() != null) {
            replyCheckin = checkinRepository.findById(request.getReplyToCheckinId()).orElse(null);
        }

        // 4. Lưu tin nhắn
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .mediaUrl(request.getMediaUrl())
                .replyToCheckin(replyCheckin) // <-- Gắn ảnh vào tin nhắn
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        // 5. Update Conversation (Để nhảy lên đầu list chat)
        conversation.setLastMessageContent(getContentPreview(message));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 6. Map to Response (Dùng Mapper)
        MessageResponse response = chatMapper.toResponse(message);

        // 7. REALTIME PUSH: Gửi riêng cho người nhận
        // Client người nhận sẽ subscribe: /user/queue/messages
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(), // Username để định danh
                "/queue/messages",
                response
        );

        return response;
    }

    @Override
    public Page<MessageResponse> getConversationMessages(Long partnerId, User currentUser, Pageable pageable) {
        // Tìm phòng chat
        Conversation conversation = conversationRepository.findByUsers(currentUser.getId(), partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện"));
        
        // Dùng Mapper với method reference
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
                .map(chatMapper::toResponse);
    }

    private Conversation createNewConversation(User u1, User u2) {
        // Luôn lưu ID nhỏ ở user1 để đảm bảo duy nhất
        User user1 = u1.getId() < u2.getId() ? u1 : u2;
        User user2 = u1.getId() < u2.getId() ? u2 : u1;
        
        return conversationRepository.save(Conversation.builder()
                .user1(user1)
                .user2(user2)
                .build());
    }

    private String getContentPreview(Message msg) {
        if (msg.getType() != null) {
            return switch (msg.getType()) {
                case IMAGE -> "[Hình ảnh]";
                case VOICE -> "[Tin nhắn thoại]";
                default -> msg.getContent();
            };
        }
        return msg.getContent();
    }
}