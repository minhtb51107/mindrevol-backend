package com.mindrevol.backend.modules.chat.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.chat.dto.event.MessageReadEvent;
import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.*;
import com.mindrevol.backend.modules.chat.mapper.ChatMapper;
import com.mindrevol.backend.modules.chat.repository.ConversationRepository;
import com.mindrevol.backend.modules.chat.repository.MessageRepository;
import com.mindrevol.backend.modules.chat.service.ChatService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserBlockService;
import com.mindrevol.backend.modules.user.service.UserPresenceService; // Import Presence Service
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserBlockService userBlockService;
    private final UserPresenceService userPresenceService; // Inject Service check online

    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        Long receiverId = request.getReceiverId();

        if (userBlockService.isBlocked(receiverId, senderId)) {
            throw new BadRequestException("Bạn không thể gửi tin nhắn cho người này.");
        }

        Conversation conversation = conversationRepository.findConversationByUsers(senderId, receiverId)
                .orElseGet(() -> createNewConversation(senderId, receiverId));

        Message message = Message.builder()
                .conversation(conversation)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .metadata(request.getMetadata())
                .clientSideId(request.getClientSideId())
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();

        message = messageRepository.save(message);

        // Update sorting inbox
        String previewContent = message.getType() == MessageType.IMAGE ? "[Hình ảnh]" : message.getContent();
        conversation.setLastMessageContent(previewContent);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastSenderId(senderId);
        
        conversationRepository.save(conversation);

        MessageResponse response = chatMapper.toResponse(message);

        // Real-time push
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/messages",
                response
        );

        return response;
    }

    private Conversation createNewConversation(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        Conversation conv = Conversation.builder()
                .user1(sender)
                .user2(receiver)
                .lastMessageAt(LocalDateTime.now())
                .build();
        
        return conversationRepository.save(conv);
    }

    // [FIX LỖI 500]: Thêm @Transactional(readOnly = true)
    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(Long userId) {
        // 1. Lấy list từ DB
        List<Conversation> conversations = conversationRepository.findByUser(userId);

        // 2. Map sang DTO
        return conversations.stream().map(conv -> {
            // Xác định ai là Partner
            User partnerEntity = conv.getUser1().getId().equals(userId) ? conv.getUser2() : conv.getUser1();
            
            // Lấy trạng thái Online thật từ Redis
            boolean isOnline = userPresenceService.isUserOnline(partnerEntity.getId());

            // [LỖI TRƯỚC ĐÂY]: partnerEntity.getFullname() gây lỗi LazyInit nếu không có Transactional
            UserSummaryResponse partnerDto = UserSummaryResponse.builder()
                .id(partnerEntity.getId())
                .fullname(partnerEntity.getFullname())
                .avatarUrl(partnerEntity.getAvatarUrl())
                .handle(partnerEntity.getHandle())
                .isOnline(isOnline)
                .build();

            long unread = messageRepository.countUnreadMessages(conv.getId(), userId);

            return ConversationResponse.builder()
                    .id(conv.getId())
                    .partner(partnerDto)
                    .lastMessageContent(conv.getLastMessageContent())
                    .lastMessageAt(conv.getLastMessageAt())
                    .lastSenderId(conv.getLastSenderId())
                    .unreadCount(unread)
                    .status(conv.getStatus() != null ? conv.getStatus().name() : "ACTIVE")
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(Long conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(chatMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesWithUser(Long currentUserId, Long partnerId, Pageable pageable) {
        Conversation conversation = conversationRepository.findConversationByUsers(currentUserId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện nào."));
        
        // Lưu ý: Không gọi hàm ghi (markRead) trong hàm đọc (readOnly) nếu không cần thiết
        // Frontend sẽ gọi API markRead riêng
        
        return getConversationMessages(conversation.getId(), pageable);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage != null) {
            boolean updated = false;
            // Cập nhật con trỏ đã đọc
            if (conversation.getUser1().getId().equals(userId)) {
                if (conversation.getUser1LastReadMessageId() == null || conversation.getUser1LastReadMessageId() < lastMessage.getId()) {
                    conversation.setUser1LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            } else if (conversation.getUser2().getId().equals(userId)) {
                if (conversation.getUser2LastReadMessageId() == null || conversation.getUser2LastReadMessageId() < lastMessage.getId()) {
                    conversation.setUser2LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            }

            if (updated) {
                conversationRepository.save(conversation);
                
                // Gửi Socket báo "Đã xem" cho đối phương
                Long partnerId = conversation.getUser1().getId().equals(userId) 
                        ? conversation.getUser2().getId() 
                        : conversation.getUser1().getId();
                
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(partnerId),
                    "/queue/read-receipt",
                    new MessageReadEvent(conversationId, lastMessage.getId(), userId)
                );
            }
        }
    }

    @Override
    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id).orElseThrow();
    }
}