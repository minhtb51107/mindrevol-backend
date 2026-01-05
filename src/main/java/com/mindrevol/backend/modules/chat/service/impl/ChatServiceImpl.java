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
    public MessageResponse sendMessage(String senderId, SendMessageRequest request) { 
        String receiverId = request.getReceiverId(); 
        
        if (receiverId == null || receiverId.isEmpty()) {
             throw new BadRequestException("Receiver ID không được để trống");
        }

        if (userBlockService.isBlocked(receiverId, senderId)) {
            throw new BadRequestException("Bạn không thể gửi tin nhắn cho người này.");
        }

        User sender = userRepository.getReferenceById(senderId);
        User receiver = userRepository.getReferenceById(receiverId);

        // [FIX] Kiểm tra hội thoại đã tồn tại chưa
        List<Conversation> existingConvs = conversationRepository.findByUsers(senderId, receiverId);
        Conversation conversation;
        if (existingConvs.isEmpty()) {
            conversation = createNewConversation(sender, receiver);
        } else {
            // Lấy cái mới nhất để tiếp tục chat
            conversation = existingConvs.get(0);
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .metadata(request.getMetadata())
                .clientSideId(request.getClientSideId())
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();

        message = messageRepository.save(message);

        String previewContent = message.getType() == MessageType.IMAGE ? "[Hình ảnh]" : message.getContent();
        conversation.setLastMessageContent(previewContent);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastSenderId(senderId);
        
        conversationRepository.save(conversation);

        MessageResponse response = chatMapper.toResponse(message);

        messagingTemplate.convertAndSendToUser(
                receiverId, 
                "/queue/messages",
                response
        );

        return response;
    }

    private Conversation createNewConversation(User sender, User receiver) {
        Conversation conv = Conversation.builder()
                .user1(sender)
                .user2(receiver)
                .lastMessageAt(LocalDateTime.now())
                .status(ConversationStatus.ACTIVE)
                .build();
        
        return conversationRepository.save(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(String userId) { 
        List<Conversation> conversations = conversationRepository.findValidConversationsByUserId(userId);

        return conversations.stream().map(conv -> {
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
    public Page<MessageResponse> getConversationMessages(String conversationId, Pageable pageable) { 
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(chatMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesWithUser(String currentUserId, String partnerId, Pageable pageable) { 
        List<Conversation> conversations = conversationRepository.findByUsers(currentUserId, partnerId);
        
        Conversation conversation = conversations.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện nào."));
        
        return getConversationMessages(conversation.getId(), pageable);
    }

    @Override
    @Transactional
    public void markConversationAsRead(String conversationId, String userId) { 
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

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

        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage != null) {
            boolean updated = false;
            
            if (conversation.getUser1().getId().equals(userId)) {
                if (conversation.getUser1LastReadMessageId() == null || 
                    conversation.getUser1LastReadMessageId().compareTo(lastMessage.getId()) < 0) {
                    conversation.setUser1LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            } else if (conversation.getUser2().getId().equals(userId)) {
                if (conversation.getUser2LastReadMessageId() == null || 
                    conversation.getUser2LastReadMessageId().compareTo(lastMessage.getId()) < 0) {
                    conversation.setUser2LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            }

            if (updated) {
                conversationRepository.save(conversation);
                
                String partnerId = conversation.getUser1().getId().equals(userId) 
                        ? conversation.getUser2().getId() 
                        : conversation.getUser1().getId();
                
                messagingTemplate.convertAndSendToUser(
                    partnerId, 
                    "/queue/read-receipt",
                    new MessageReadEvent(conversationId, lastMessage.getId(), userId)
                );
            }
        }
    }

    @Override
    public Conversation getConversationById(String id) { 
        return conversationRepository.findById(id).orElseThrow();
    }
    
    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(String senderId, String receiverId) { 
        if (userBlockService.isBlocked(receiverId, senderId) || userBlockService.isBlocked(senderId, receiverId)) {
            throw new BadRequestException("Không thể bắt đầu cuộc trò chuyện do chặn người dùng.");
        }
        
        User sender = userRepository.getReferenceById(senderId);
        User receiver = userRepository.getReferenceById(receiverId);

        // [FIX] Xử lý duplicate conversation
        List<Conversation> existingConvs = conversationRepository.findByUsers(senderId, receiverId);
        Conversation conversation;
        
        if (existingConvs.isEmpty()) {
             conversation = createNewConversation(sender, receiver);
        } else {
             conversation = existingConvs.get(0);
        }

        return mapToConversationResponse(conversation, senderId);
    }

    private ConversationResponse mapToConversationResponse(Conversation conv, String currentUserId) {
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