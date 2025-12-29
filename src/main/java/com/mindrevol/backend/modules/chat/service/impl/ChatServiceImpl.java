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
import com.mindrevol.backend.modules.user.service.UserPresenceService; 
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
    private final UserPresenceService userPresenceService; 

    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        // Giả sử request có receiverId. Nếu request có conversationId thì cần logic khác.
        Long receiverId = request.getReceiverId(); 
        
        if (receiverId == null) {
             throw new BadRequestException("Receiver ID không được để trống");
        }

        if (userBlockService.isBlocked(receiverId, senderId)) {
            throw new BadRequestException("Bạn không thể gửi tin nhắn cho người này.");
        }

        // 1. Lấy User Object (Proxy) để gán vào Message Entity
        User sender = userRepository.getReferenceById(senderId);
        User receiver = userRepository.getReferenceById(receiverId);

        // 2. Tìm hoặc tạo Conversation
        // Lưu ý: findByUsers trả về Conversation Entity
        Conversation conversation = conversationRepository.findByUsers(senderId, receiverId)
                .orElseGet(() -> createNewConversation(sender, receiver)); // [FIX] Truyền User object vào

        // 3. Tạo Message
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)      // [FIX] .senderId(Long) -> .sender(User)
                .receiver(receiver)  // [FIX] .receiverId(Long) -> .receiver(User)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .metadata(request.getMetadata())
                .clientSideId(request.getClientSideId())
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();

        message = messageRepository.save(message);

        // 4. Update Conversation Metadata
        String previewContent = message.getType() == MessageType.IMAGE ? "[Hình ảnh]" : message.getContent();
        conversation.setLastMessageContent(previewContent);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastSenderId(senderId);
        
        conversationRepository.save(conversation);

        // 5. Response & Socket
        MessageResponse response = chatMapper.toResponse(message);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/messages",
                response
        );

        return response;
    }

    // [FIX] Nhận User object thay vì Long để tránh query lại DB
    private Conversation createNewConversation(User sender, User receiver) {
        Conversation conv = Conversation.builder()
                .user1(sender)
                .user2(receiver)
                .lastMessageAt(LocalDateTime.now())
                .status(ConversationStatus.ACTIVE) // Đảm bảo có status
                .build();
        
        return conversationRepository.save(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findValidConversationsByUserId(userId);

        return conversations.stream().map(conv -> {
            // Xác định partner
            User partnerEntity = conv.getUser1().getId().equals(userId) ? conv.getUser2() : conv.getUser1();
            
            boolean isOnline = userPresenceService.isUserOnline(partnerEntity.getId());

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
        Conversation conversation = conversationRepository.findByUsers(currentUserId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện nào."));
        
        return getConversationMessages(conversation.getId(), pageable);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // 1. Cập nhật Status các tin nhắn chưa đọc
        List<Message> unreadMessages = messageRepository.findUnreadMessagesInConversation(
                conversationId, 
                userId, 
                MessageDeliveryStatus.SEEN
        );

        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(msg -> {
                msg.setDeliveryStatus(MessageDeliveryStatus.SEEN);
            });
            messageRepository.saveAll(unreadMessages);
        }

        // 2. Cập nhật LastReadMessageId
        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage != null) {
            boolean updated = false;
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
    
    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(Long senderId, Long receiverId) {
        if (userBlockService.isBlocked(receiverId, senderId) || userBlockService.isBlocked(senderId, receiverId)) {
            throw new BadRequestException("Không thể bắt đầu cuộc trò chuyện do chặn người dùng.");
        }
        
        // Dùng getReferenceById để tối ưu nếu cần tạo mới
        User sender = userRepository.getReferenceById(senderId);
        User receiver = userRepository.getReferenceById(receiverId);

        Conversation conversation = conversationRepository.findByUsers(senderId, receiverId)
                .orElseGet(() -> createNewConversation(sender, receiver));

        return mapToConversationResponse(conversation, senderId);
    }

    private ConversationResponse mapToConversationResponse(Conversation conv, Long currentUserId) {
        User partnerEntity = conv.getUser1().getId().equals(currentUserId) ? conv.getUser2() : conv.getUser1();
        
        boolean isOnline = userPresenceService.isUserOnline(partnerEntity.getId());

        UserSummaryResponse partnerDto = UserSummaryResponse.builder()
            .id(partnerEntity.getId())
            .fullname(partnerEntity.getFullname())
            .avatarUrl(partnerEntity.getAvatarUrl())
            .handle(partnerEntity.getHandle())
            .isOnline(isOnline)
            .build();

        long unread = messageRepository.countUnreadMessages(conv.getId(), currentUserId);

        return ConversationResponse.builder()
                .id(conv.getId())
                .partner(partnerDto)
                .lastMessageContent(conv.getLastMessageContent())
                .lastMessageAt(conv.getLastMessageAt())
                .lastSenderId(conv.getLastSenderId())
                .unreadCount(unread)
                .status(conv.getStatus() != null ? conv.getStatus().name() : "ACTIVE")
                .build();
    }
}